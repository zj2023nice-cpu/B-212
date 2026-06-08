package com.milktea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.common.Result;
import com.milktea.entity.*;
import com.milktea.enums.DeliveryType;
import com.milktea.enums.OrderStatus;
import com.milktea.mapper.*;
import com.milktea.service.ProductService;
import com.milktea.service.UserService;
import com.milktea.service.CouponService;
import com.milktea.service.MemberService;
import com.milktea.service.AddressService;
import com.milktea.service.NotificationService;
import com.milktea.statemachine.OrderStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private static final int MAX_RETRY_TIMES = 3;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private OrderCancelLogMapper orderCancelLogMapper;

    @Autowired
    private NotificationService notificationService;

    private Long getCurrentUserId() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByUsername(username).getId();
    }

    private boolean isCurrentUserAdmin() {
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Order> createOrder(@RequestBody Order orderReq) {
        Long userId = getCurrentUserId();
        
        List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        if (cartItems.isEmpty()) {
            return Result.error("Cart is empty");
        }

        for (CartItem item : cartItems) {
            productService.checkStock(item.getProductId(), item.getQuantity());
        }

        Map<Long, BigDecimal> verifiedPrices = new HashMap<>();
        for (CartItem item : cartItems) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                return Result.error("商品不存在: " + item.getProductId());
            }
            BigDecimal recalculated = productService.calculateUnitPrice(product, item.getSpecs());
            if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(recalculated) != 0) {
                logger.warn("下单价格校验失败: 购物车价格={}, 后端重算价格={}, productId={}, specs={}",
                        item.getUnitPrice(), recalculated, item.getProductId(), item.getSpecs());
                return Result.error("商品 [" + product.getName() + "] 价格已变动，请刷新购物车后重试");
            }
            verifiedPrices.put(item.getId(), recalculated);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            BigDecimal itemPrice = verifiedPrices.getOrDefault(item.getId(),
                    item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO);
            totalAmount = totalAmount.add(itemPrice.multiply(new BigDecimal(item.getQuantity())));
        }

        for (CartItem item : cartItems) {
            boolean deducted = productService.deductStockWithRetry(
                    item.getProductId(), 
                    item.getQuantity(), 
                    MAX_RETRY_TIMES
            );
            if (!deducted) {
                Product product = productMapper.selectById(item.getProductId());
                String productName = product != null ? product.getName() : "未知商品";
                logger.error("库存扣减失败，商品: {}, 数量: {}", productName, item.getQuantity());
                throw new RuntimeException("商品 [" + productName + "] 库存不足，请刷新后重试");
            }
        }

        BigDecimal couponDiscountAmount = BigDecimal.ZERO;
        Long userCouponId = orderReq.getUserCouponId();

        if (userCouponId != null) {
            try {
                couponDiscountAmount = couponService.calculateDiscount(userCouponId, totalAmount);
                if (couponDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    userCouponId = null;
                }
            } catch (Exception e) {
                logger.warn("优惠券计算失败，将不使用优惠券: {}", e.getMessage());
                userCouponId = null;
                couponDiscountAmount = BigDecimal.ZERO;
            }
        }

        BigDecimal afterCouponAmount = totalAmount.subtract(couponDiscountAmount);
        BigDecimal memberDiscountAmount = memberService.calculateDiscount(userId, afterCouponAmount);
        BigDecimal discountAmount = couponDiscountAmount.add(memberDiscountAmount);

        BigDecimal payAmount = totalAmount.subtract(discountAmount);

        Order order = new Order();
        order.setOrderSn(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(payAmount);
        order.setUserCouponId(userCouponId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setRemark(orderReq.getRemark());

        DeliveryType deliveryType = orderReq.getDeliveryType();
        if (deliveryType == null) {
            deliveryType = DeliveryType.DELIVERY;
        }
        order.setDeliveryType(deliveryType);

        if (deliveryType == DeliveryType.DELIVERY) {
            if (orderReq.getAddressId() == null) {
                return Result.error("外卖配送请选择收货地址");
            }
            try {
                Address address = addressService.getByIdAndUserId(orderReq.getAddressId(), userId);
                order.setAddressId(address.getId());
                order.setAddressContactName(address.getContactName());
                order.setAddressPhone(address.getPhone());
                String fullAddr = address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress();
                order.setAddressFull(fullAddr);
                order.setAddress(fullAddr);
            } catch (Exception e) {
                logger.warn("地址信息获取失败: addressId={}, reason={}", orderReq.getAddressId(), e.getMessage());
                return Result.error("收货地址信息获取失败，请重新选择");
            }
        } else if (deliveryType == DeliveryType.SELF_PICKUP) {
            if (orderReq.getPickupStore() == null || orderReq.getPickupStore().trim().isEmpty()) {
                return Result.error("门店自提请选择自提门店");
            }
            if (orderReq.getPickupTime() == null) {
                return Result.error("门店自提请选择预计自提时间");
            }
            LocalTime businessStart = LocalTime.of(9, 0);
            LocalTime businessEnd = LocalTime.of(22, 0);
            LocalTime pickupLocalTime = orderReq.getPickupTime().toLocalTime();
            if (pickupLocalTime.isBefore(businessStart) || !pickupLocalTime.isBefore(businessEnd)) {
                return Result.error("自提时间需在营业时间 09:00-22:00 内");
            }
            LocalDateTime minPickupTime = LocalDateTime.now().plusMinutes(30);
            if (orderReq.getPickupTime().isBefore(minPickupTime)) {
                return Result.error("自提时间需在当前时间30分钟之后");
            }
            order.setPickupStore(orderReq.getPickupStore().trim());
            order.setPickupTime(orderReq.getPickupTime());
        }

        orderMapper.insert(order);

        for (CartItem item : cartItems) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                logger.error("创建订单项时商品不存在: productId={}", item.getProductId());
                throw new IllegalArgumentException("商品不存在: " + item.getProductId());
            }
            BigDecimal verifiedPrice = verifiedPrices.getOrDefault(item.getId(),
                    item.getUnitPrice() != null ? item.getUnitPrice() : product.getPrice());
            OrderItem detail = new OrderItem();
            detail.setOrderId(order.getId());
            detail.setProductId(item.getProductId());
            detail.setProductName(product.getName());
            detail.setProductPrice(verifiedPrice);
            detail.setQuantity(item.getQuantity());
            detail.setSpecs(item.getSpecs());
            orderItemMapper.insert(detail);
            cartItemMapper.deleteById(item.getId());
        }

        boolean couponRedeemed = false;
        if (userCouponId != null) {
            try {
                couponService.useCoupon(userCouponId, userId, order.getId());
                couponRedeemed = true;
            } catch (Exception e) {
                logger.warn("优惠券核销失败，订单将按无优惠创建: userCouponId={}, orderId={}, reason={}", userCouponId, order.getId(), e.getMessage());
            }
        }

        if (userCouponId != null && !couponRedeemed) {
            discountAmount = memberDiscountAmount;
            payAmount = totalAmount.subtract(discountAmount);
            order.setDiscountAmount(discountAmount);
            order.setPayAmount(payAmount);
            order.setUserCouponId(null);
            orderMapper.updateById(order);
        }

        logger.info("订单创建成功(待支付): orderId={}, userId={}, totalAmount={}, discountAmount={}, payAmount={}, couponRedeemed={}, memberDiscount={}", 
                order.getId(), userId, totalAmount, couponRedeemed ? discountAmount : BigDecimal.ZERO, 
                couponRedeemed ? payAmount : totalAmount, couponRedeemed, memberDiscountAmount);

        try {
            notificationService.sendNotification(userId, "订单创建成功",
                    "您的订单 " + order.getOrderSn() + " 已创建成功，待支付金额 ¥" + order.getPayAmount(),
                    "ORDER", order.getId());
        } catch (Exception e) {
            logger.warn("发送订单创建通知失败: orderId={}, reason={}", order.getId(), e.getMessage());
        }

        return Result.success(order);
    }

    @GetMapping
    public Result<Page<Order>> getMyOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderSn,
            @RequestParam(required = false) String productName) {
        Long userId = getCurrentUserId();
        Page<Order> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status);
                wrapper.eq(Order::getStatus, orderStatus);
            } catch (IllegalArgumentException e) {
                return Result.error("Invalid order status");
            }
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(Order::getCreateTime, java.time.LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(Order::getCreateTime, java.time.LocalDate.parse(endDate).atTime(java.time.LocalTime.MAX));
        }
        if (orderSn != null && !orderSn.isEmpty()) {
            wrapper.like(Order::getOrderSn, orderSn);
        }
        if (productName != null && !productName.isEmpty()) {
            wrapper.exists("SELECT 1 FROM order_items WHERE order_items.order_id = orders.id AND product_name LIKE {0}",
                    "%" + productName + "%");
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return Result.success(orderMapper.selectPage(pageParam, wrapper));
    }

    @GetMapping("/{id}")
    public Result<Order> getOrderDetail(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        boolean isAdmin = isCurrentUserAdmin();
        Order order = orderMapper.selectById(id);
        
        if (order == null) {
            return Result.error("Order not found");
        }
        
        if (!isAdmin && !order.getUserId().equals(currentUserId)) {
            return Result.error("Not authorized to view this order");
        }
        
        return Result.success(order);
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<Order>> adminListOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderSn,
            @RequestParam(required = false) String productName) {
        Page<Order> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status);
                wrapper.eq(Order::getStatus, orderStatus);
            } catch (IllegalArgumentException e) {
                return Result.error("Invalid order status");
            }
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(Order::getCreateTime, java.time.LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(Order::getCreateTime, java.time.LocalDate.parse(endDate).atTime(java.time.LocalTime.MAX));
        }
        if (orderSn != null && !orderSn.isEmpty()) {
            wrapper.like(Order::getOrderSn, orderSn);
        }
        if (productName != null && !productName.isEmpty()) {
            wrapper.exists("SELECT 1 FROM order_items WHERE order_items.order_id = orders.id AND product_name LIKE {0}",
                    "%" + productName + "%");
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return Result.success(orderMapper.selectPage(pageParam, wrapper));
    }

    @GetMapping("/{id}/items")
    public Result<List<OrderItem>> getOrderItems(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        boolean isAdmin = isCurrentUserAdmin();
        Order order = orderMapper.selectById(id);
        
        if (order == null) {
            return Result.error("Order not found");
        }
        
        if (!isAdmin && !order.getUserId().equals(currentUserId)) {
            return Result.error("Not authorized to view this order's items");
        }
        
        return Result.success(orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id)));
    }

    @PutMapping("/{id}/status")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Long currentUserId = getCurrentUserId();
        boolean isAdmin = isCurrentUserAdmin();
        Order existingOrder = orderMapper.selectById(id);
        
        if (existingOrder == null) {
            return Result.error("Order not found");
        }
        
        if (!isAdmin && !existingOrder.getUserId().equals(currentUserId)) {
            return Result.error("Not authorized to update this order");
        }

        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return Result.error("Invalid order status");
        }
        
        OrderStatus currentStatus = existingOrder.getStatus();
        
        if (currentStatus.isTerminal()) {
            return Result.error("Cannot update status of " + currentStatus.getDescription() + " order");
        }
        
        if (!isAdmin) {
            if (!OrderStateMachine.isUserAllowedTransition(currentStatus, targetStatus)) {
                return Result.error("Invalid status transition");
            }
        } else {
            if (!OrderStateMachine.canTransit(currentStatus, targetStatus)) {
                return Result.error("Invalid status transition");
            }
        }

        boolean notificationSent = false;

        if (targetStatus == OrderStatus.PAID && currentStatus == OrderStatus.PENDING_PAYMENT) {
            Order refreshed = orderMapper.selectById(id);
            if (refreshed.getStatus() != OrderStatus.PENDING_PAYMENT) {
                return Result.error("Order status has changed, please refresh");
            }
            try {
                memberService.earnPoints(existingOrder.getUserId(), id, existingOrder.getPayAmount());
            } catch (Exception e) {
                logger.warn("支付后积分发放失败，不影响订单: orderId={}, reason={}", id, e.getMessage());
            }
            logger.info("订单已支付: orderId={}, userId={}", id, existingOrder.getUserId());
            try {
                notificationService.sendNotification(existingOrder.getUserId(), "订单已支付",
                        "您的订单 " + existingOrder.getOrderSn() + " 已支付成功",
                        "ORDER", id);
                notificationSent = true;
            } catch (Exception e) {
                logger.warn("发送订单支付通知失败: orderId={}, reason={}", id, e.getMessage());
            }
        }

        if (targetStatus == OrderStatus.CANCELLED) {
            try {
                restoreOrderStock(id);
                if (currentStatus != OrderStatus.PENDING_PAYMENT) {
                    try {
                        memberService.deductPoints(existingOrder.getUserId(), id, existingOrder.getPayAmount());
                    } catch (Exception e) {
                        logger.warn("积分扣减失败，不影响取消订单: orderId={}, reason={}", id, e.getMessage());
                    }
                }
                String cancelReason = "用户主动取消";
                Order orderToUpdate = new Order();
                orderToUpdate.setId(id);
                orderToUpdate.setStatus(targetStatus);
                orderToUpdate.setCancelReason(cancelReason);
                orderMapper.updateById(orderToUpdate);

                OrderCancelLog cancelLog = new OrderCancelLog();
                cancelLog.setOrderId(id);
                cancelLog.setCancelReason(cancelReason);
                cancelLog.setOperator("USER");
                orderCancelLogMapper.insert(cancelLog);

                try {
                    notificationService.sendNotification(existingOrder.getUserId(), "订单已取消",
                            "您的订单 " + existingOrder.getOrderSn() + " 已取消",
                            "ORDER", id);
                } catch (Exception e2) {
                    logger.warn("发送订单取消通知失败: orderId={}, reason={}", id, e2.getMessage());
                }

                logger.info("订单已取消，所有商品库存已恢复: orderId={}, cancelReason={}", id, cancelReason);
                return Result.success("Status updated");
            } catch (Exception e) {
                logger.error("取消订单失败: orderId={}, 原因={}", id, e.getMessage());
                throw e;
            }
        }
        
        Order order = new Order();
        order.setId(id);
        order.setStatus(targetStatus);
        orderMapper.updateById(order);

        if (!notificationSent) {
            try {
                notificationService.sendNotification(existingOrder.getUserId(), "订单状态变更",
                        "您的订单 " + existingOrder.getOrderSn() + " 状态已更新为：" + targetStatus.getDescription(),
                        "ORDER", id);
            } catch (Exception e) {
                logger.warn("发送订单状态变更通知失败: orderId={}, reason={}", id, e.getMessage());
            }
        }

        return Result.success("Status updated");
    }

    private void restoreOrderStock(Long orderId) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        
        int successCount = 0;
        int failCount = 0;
        StringBuilder failDetails = new StringBuilder();
        
        for (OrderItem item : orderItems) {
            boolean restored = productService.restoreStock(item.getProductId(), item.getQuantity());
            if (restored) {
                successCount++;
            } else {
                failCount++;
                String detail = String.format("productId=%d, productName=%s, quantity=%d", 
                        item.getProductId(), item.getProductName(), item.getQuantity());
                failDetails.append(failDetails.length() > 0 ? "; " : "").append(detail);
                logger.error("订单商品库存恢复失败: orderId={}, {}", orderId, detail);
            }
        }
        
        logger.info("订单库存恢复完成: orderId={}, 总商品数={}, 成功={}, 失败={}", 
                orderId, orderItems.size(), successCount, failCount);
        
        if (failCount > 0) {
            String errorMsg = String.format("无法取消订单：%d个商品库存恢复失败，失败详情：%s", 
                    failCount, failDetails.toString());
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }
}

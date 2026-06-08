package com.milktea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.common.Result;
import com.milktea.entity.*;
import com.milktea.mapper.*;
import com.milktea.service.ProductService;
import com.milktea.service.UserService;
import com.milktea.service.CouponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
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

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                logger.error("创建订单时商品不存在: productId={}", item.getProductId());
                throw new IllegalArgumentException("商品不存在: " + item.getProductId());
            }
            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
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

        BigDecimal discountAmount = BigDecimal.ZERO;
        Long userCouponId = orderReq.getUserCouponId();

        if (userCouponId != null) {
            try {
                discountAmount = couponService.calculateDiscount(userCouponId, totalAmount);
                if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    userCouponId = null;
                }
            } catch (Exception e) {
                logger.warn("优惠券计算失败，将不使用优惠券: {}", e.getMessage());
                userCouponId = null;
                discountAmount = BigDecimal.ZERO;
            }
        }

        BigDecimal payAmount = totalAmount.subtract(discountAmount);

        Order order = new Order();
        order.setOrderSn(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(payAmount);
        order.setUserCouponId(userCouponId);
        order.setStatus(1);
        order.setRemark(orderReq.getRemark());
        orderMapper.insert(order);

        for (CartItem item : cartItems) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                logger.error("创建订单项时商品不存在: productId={}", item.getProductId());
                throw new IllegalArgumentException("商品不存在: " + item.getProductId());
            }
            OrderItem detail = new OrderItem();
            detail.setOrderId(order.getId());
            detail.setProductId(item.getProductId());
            detail.setProductName(product.getName());
            detail.setProductPrice(product.getPrice());
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
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setPayAmount(totalAmount);
            order.setUserCouponId(null);
            orderMapper.updateById(order);
        }

        logger.info("订单创建成功: orderId={}, userId={}, totalAmount={}, discountAmount={}, payAmount={}, couponRedeemed={}", 
                order.getId(), userId, totalAmount, couponRedeemed ? discountAmount : BigDecimal.ZERO, 
                couponRedeemed ? payAmount : totalAmount, couponRedeemed);
        return Result.success(order);
    }

    @GetMapping
    public Result<Page<Order>> getMyOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Order> pageParam = new Page<>(page, pageSize);
        return Result.success(orderMapper.selectPage(pageParam, new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, getCurrentUserId())
                .orderByDesc(Order::getCreateTime)));
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
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Long currentUserId = getCurrentUserId();
        boolean isAdmin = isCurrentUserAdmin();
        Order existingOrder = orderMapper.selectById(id);
        
        if (existingOrder == null) {
            return Result.error("Order not found");
        }
        
        if (!isAdmin && !existingOrder.getUserId().equals(currentUserId)) {
            return Result.error("Not authorized to update this order");
        }
        
        if (status < 0 || status > 5) {
            return Result.error("Invalid order status");
        }
        
        Integer currentStatus = existingOrder.getStatus();
        
        if (currentStatus == 3) {
            return Result.error("Cannot update status of cancelled order");
        }
        
        if (currentStatus == 5) {
            return Result.error("Cannot update status of reviewed order");
        }
        
        if (!isAdmin) {
            boolean isAllowedTransition = false;
            
            if (status == 3 && (currentStatus == 0 || currentStatus == 1 || currentStatus == 2)) {
                isAllowedTransition = true;
            }
            
            if (status == 4 && currentStatus == 2) {
                isAllowedTransition = true;
            }
            
            if (!isAllowedTransition) {
                return Result.error("Invalid status transition");
            }
        }

        if (status == 3 && currentStatus != 3) {
            try {
                restoreOrderStock(id);
                logger.info("订单已取消，所有商品库存已恢复: orderId={}", id);
            } catch (Exception e) {
                logger.error("取消订单失败: orderId={}, 原因={}", id, e.getMessage());
                throw e;
            }
        }
        
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        orderMapper.updateById(order);
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

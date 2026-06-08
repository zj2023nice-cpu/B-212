package com.milktea.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.milktea.config.OrderTimeoutProperties;
import com.milktea.entity.Order;
import com.milktea.entity.OrderCancelLog;
import com.milktea.entity.OrderItem;
import com.milktea.mapper.OrderCancelLogMapper;
import com.milktea.mapper.OrderItemMapper;
import com.milktea.mapper.OrderMapper;
import com.milktea.service.MemberService;
import com.milktea.service.OrderTimeoutService;
import com.milktea.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    private static final Logger logger = LoggerFactory.getLogger(OrderTimeoutServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderCancelLogMapper orderCancelLogMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private OrderTimeoutProperties orderTimeoutProperties;

    @Override
    public int cancelExpiredUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(orderTimeoutProperties.getMinutes());
        List<Order> expiredOrders = orderMapper.findExpiredUnpaidOrders(deadline);

        if (expiredOrders.isEmpty()) {
            logger.debug("未发现超时未支付订单");
            return 0;
        }

        logger.info("发现 {} 笔超时未支付订单，开始处理", expiredOrders.size());

        int cancelledCount = 0;
        for (Order order : expiredOrders) {
            try {
                boolean cancelled = cancelSingleOrder(order);
                if (cancelled) {
                    cancelledCount++;
                }
            } catch (Exception e) {
                logger.error("超时取消订单异常: orderId={}, error={}", order.getId(), e.getMessage(), e);
            }
        }

        logger.info("超时取消订单处理完成: 发现={}, 成功取消={}", expiredOrders.size(), cancelledCount);
        return cancelledCount;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelSingleOrder(Order order) {
        String cancelReason = orderTimeoutProperties.getCancelReason();

        int updatedRows = orderMapper.cancelExpiredOrder(order.getId(), cancelReason);
        if (updatedRows == 0) {
            logger.info("订单已被其他实例处理，跳过: orderId={}", order.getId());
            return false;
        }

        logger.info("订单状态已更新为取消: orderId={}, cancelReason={}", order.getId(), cancelReason);

        restoreOrderStock(order.getId());

        if (order.getStatus() != 0) {
            try {
                memberService.deductPoints(order.getUserId(), order.getId(), order.getPayAmount());
            } catch (Exception e) {
                logger.warn("超时取消订单扣减积分失败，不影响取消: orderId={}, reason={}", order.getId(), e.getMessage());
            }
        }

        recordCancelLog(order.getId(), cancelReason, "SYSTEM");

        logger.info("超时未支付订单自动取消完成: orderId={}, userId={}, cancelReason={}", 
                order.getId(), order.getUserId(), cancelReason);
        return true;
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
                logger.error("超时取消-库存恢复失败: orderId={}, {}", orderId, detail);
            }
        }

        logger.info("超时取消-库存恢复完成: orderId={}, 总商品数={}, 成功={}, 失败={}",
                orderId, orderItems.size(), successCount, failCount);

        if (failCount > 0) {
            String errorMsg = String.format("库存恢复失败，订单取消回滚: orderId=%d, 失败%d个商品, 详情: %s",
                    orderId, failCount, failDetails.toString());
            throw new RuntimeException(errorMsg);
        }
    }

    private void recordCancelLog(Long orderId, String cancelReason, String operator) {
        try {
            OrderCancelLog cancelLog = new OrderCancelLog();
            cancelLog.setOrderId(orderId);
            cancelLog.setCancelReason(cancelReason);
            cancelLog.setOperator(operator);
            orderCancelLogMapper.insert(cancelLog);
            logger.info("取消日志记录成功: orderId={}, operator={}", orderId, operator);
        } catch (DuplicateKeyException e) {
            logger.info("取消日志已存在，跳过重复记录: orderId={}", orderId);
        }
    }
}

package com.milktea.scheduler;

import com.milktea.service.OrderTimeoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    @Autowired
    private OrderTimeoutService orderTimeoutService;

    @Scheduled(fixedDelayString = "${order.timeout.cron-interval-seconds:60}000")
    public void cancelExpiredOrders() {
        logger.debug("开始扫描超时未支付订单...");
        try {
            int cancelledCount = orderTimeoutService.cancelExpiredUnpaidOrders();
            if (cancelledCount > 0) {
                logger.info("本次扫描取消了 {} 笔超时未支付订单", cancelledCount);
            }
        } catch (Exception e) {
            logger.error("超时取消订单定时任务执行异常: {}", e.getMessage(), e);
        }
    }
}

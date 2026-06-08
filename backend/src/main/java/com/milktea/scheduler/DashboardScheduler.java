package com.milktea.scheduler;

import com.milktea.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DashboardScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DashboardScheduler.class);

    @Autowired
    private DashboardService dashboardService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void precomputeDashboardData() {
        logger.info("定时预计算仪表盘数据开始");
        try {
            dashboardService.refreshCache();
            logger.info("定时预计算仪表盘数据完成");
        } catch (Exception e) {
            logger.error("定时预计算仪表盘数据异常: {}", e.getMessage(), e);
        }
    }
}

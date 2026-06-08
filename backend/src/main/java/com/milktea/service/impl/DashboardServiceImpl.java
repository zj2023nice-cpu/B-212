package com.milktea.service.impl;

import com.milktea.dto.DailySalesVO;
import com.milktea.dto.DashboardVO;
import com.milktea.dto.TopProductVO;
import com.milktea.mapper.OrderItemMapper;
import com.milktea.mapper.OrderMapper;
import com.milktea.mapper.UserMapper;
import com.milktea.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final AtomicReference<CachedDashboard> cachedDashboard = new AtomicReference<>();

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private UserMapper userMapper;

    private static class CachedDashboard {
        final DashboardVO data;
        final long timestamp;

        CachedDashboard(DashboardVO data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    @Override
    public DashboardVO getDashboardData() {
        CachedDashboard cached = cachedDashboard.get();
        if (cached != null && !cached.isExpired()) {
            logger.debug("仪表盘缓存命中");
            return cached.data;
        }

        logger.info("仪表盘缓存未命中，查询数据库");
        DashboardVO data = buildDashboardData();
        cachedDashboard.set(new CachedDashboard(data));
        return data;
    }

    @Override
    public void refreshCache() {
        logger.info("定时任务刷新仪表盘缓存");
        DashboardVO data = buildDashboardData();
        cachedDashboard.set(new CachedDashboard(data));
    }

    private DashboardVO buildDashboardData() {
        DashboardVO vo = new DashboardVO();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        vo.setTodayOrderCount(orderMapper.countTodayOrders(todayStart, tomorrowStart));
        vo.setTodaySalesAmount(orderMapper.sumTodaySales(todayStart, tomorrowStart));
        vo.setPendingOrderCount(orderMapper.countPendingOrders());
        vo.setRegisteredUserCount(userMapper.countAllUsers());

        LocalDateTime weekAgo = LocalDate.now().minusDays(6).atStartOfDay();
        List<DailySalesVO> rawTrend = orderMapper.selectDailySalesTrend(weekAgo);
        vo.setWeeklyTrend(fillMissingDays(rawTrend, weekAgo));

        vo.setTopProducts(orderItemMapper.selectTopProducts(weekAgo, 5));

        return vo;
    }

    private List<DailySalesVO> fillMissingDays(List<DailySalesVO> rawTrend, LocalDateTime startDate) {
        Map<String, DailySalesVO> trendMap = rawTrend.stream()
                .collect(Collectors.toMap(DailySalesVO::getDate, d -> d));

        List<DailySalesVO> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i).toLocalDate();
            String dateStr = date.format(DATE_FMT);
            DailySalesVO existing = trendMap.get(dateStr);
            if (existing != null) {
                result.add(existing);
            } else {
                DailySalesVO empty = new DailySalesVO();
                empty.setDate(dateStr);
                empty.setOrderCount(0);
                empty.setSalesAmount(BigDecimal.ZERO);
                result.add(empty);
            }
        }
        return result;
    }
}

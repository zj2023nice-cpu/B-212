package com.milktea.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardVO {
    private Long todayOrderCount;
    private BigDecimal todaySalesAmount;
    private Long pendingOrderCount;
    private Long registeredUserCount;
    private List<DailySalesVO> weeklyTrend;
    private List<TopProductVO> topProducts;
}

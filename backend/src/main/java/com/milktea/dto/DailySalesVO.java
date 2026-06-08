package com.milktea.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DailySalesVO {
    private String date;
    private Integer orderCount;
    private BigDecimal salesAmount;
}

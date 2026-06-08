package com.milktea.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TopProductVO {
    private Long productId;
    private String productName;
    private String image;
    private Integer totalSales;
    private BigDecimal totalRevenue;
}

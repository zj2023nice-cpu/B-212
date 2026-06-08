package com.milktea.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionCalculateResponse {
    private BigDecimal orderAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private boolean applied;
    private Long promotionId;
    private String promotionName;
}

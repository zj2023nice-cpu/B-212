package com.milktea.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionCalculateRequest {
    private BigDecimal orderAmount;
}

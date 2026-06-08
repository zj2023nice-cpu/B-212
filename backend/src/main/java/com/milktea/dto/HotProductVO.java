package com.milktea.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class HotProductVO {
    private Long productId;
    private String productName;
    private BigDecimal price;
    private String image;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Integer totalSales;
    private String specPriceRules;
}

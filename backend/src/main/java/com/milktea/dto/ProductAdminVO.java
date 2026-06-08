package com.milktea.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductAdminVO {
    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private String image;
    private Integer status;
    private Integer stock;
    private Integer lowStockThreshold;
    private Boolean lowStock;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

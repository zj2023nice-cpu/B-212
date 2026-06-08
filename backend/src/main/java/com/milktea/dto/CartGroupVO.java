package com.milktea.dto;

import com.milktea.entity.CartItem;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CartGroupVO {
    private Long productId;
    private String productName;
    private String image;
    private BigDecimal price;
    private List<CartItem> specs;
}

package com.milktea.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ReceiptVO {
    private String storeName;
    private String storePhone;
    private String orderSn;
    private String orderTime;
    private String deliveryType;
    private String pickupStore;
    private String pickupTime;
    private String contactName;
    private String contactPhone;
    private String address;
    private String remark;
    private List<ReceiptItemVO> items;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;

    @Data
    public static class ReceiptItemVO {
        private String productName;
        private String specs;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}

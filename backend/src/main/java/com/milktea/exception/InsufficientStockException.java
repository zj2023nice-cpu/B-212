package com.milktea.exception;

public class InsufficientStockException extends RuntimeException {
    private final String productName;
    private final Integer requestedQuantity;
    private final Integer availableStock;

    public InsufficientStockException(String productName, Integer requestedQuantity, Integer availableStock) {
        super(String.format("商品 [%s] 库存不足，当前库存: %d, 请求数量: %d", 
                productName, availableStock, requestedQuantity));
        this.productName = productName;
        this.requestedQuantity = requestedQuantity;
        this.availableStock = availableStock;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }
}

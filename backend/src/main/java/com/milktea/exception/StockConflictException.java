package com.milktea.exception;

public class StockConflictException extends RuntimeException {
    public StockConflictException(String message) {
        super(message);
    }

    public StockConflictException(String productName, int retryCount) {
        super(String.format("商品 [%s] 下单时发生冲突，请稍后重试 (已尝试 %d 次)", productName, retryCount));
    }
}

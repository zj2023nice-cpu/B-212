package com.milktea.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.milktea.entity.Product;

public interface ProductService extends IService<Product> {
    void checkStock(Long productId, int quantity);
    boolean deductStock(Long productId, int quantity);
    boolean deductStockWithRetry(Long productId, int quantity, int maxRetries);
    boolean restoreStock(Long productId, int quantity);
}

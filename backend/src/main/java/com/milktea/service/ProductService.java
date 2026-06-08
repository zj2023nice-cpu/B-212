package com.milktea.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.milktea.dto.ProductAdminVO;
import com.milktea.entity.Product;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService extends IService<Product> {
    void checkStock(Long productId, int quantity);
    boolean deductStock(Long productId, int quantity);
    boolean deductStockWithRetry(Long productId, int quantity, int maxRetries);
    boolean restoreStock(Long productId, int quantity);
    List<ProductAdminVO> getLowStockProducts();
    void checkAndLogLowStock(Long productId);
    BigDecimal calculateUnitPrice(Product product, String specsJson);
}

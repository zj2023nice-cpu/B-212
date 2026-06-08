package com.milktea.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.milktea.dto.ProductAdminVO;
import com.milktea.entity.Product;
import com.milktea.exception.InsufficientStockException;
import com.milktea.mapper.ProductMapper;
import com.milktea.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public boolean save(Product product) {
        if (product.getLowStockThreshold() == null) {
            product.setLowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD);
        }
        return super.save(product);
    }

    @Override
    public boolean updateById(Product product) {
        if (product.getLowStockThreshold() == null) {
            Product existing = this.getById(product.getId());
            if (existing != null && existing.getLowStockThreshold() != null) {
                product.setLowStockThreshold(existing.getLowStockThreshold());
            } else {
                product.setLowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD);
            }
        }
        return super.updateById(product);
    }

    @Override
    public void checkStock(Long productId, int quantity) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在: " + productId);
        }
        
        Integer stock = product.getStock();
        if (stock == null) {
            stock = 0;
        }
        
        if (stock < quantity) {
            throw new InsufficientStockException(product.getName(), quantity, stock);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("扣减数量必须大于0");
        }

        Product product = this.getById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在: " + productId);
        }

        int updatedRows = productMapper.deductStock(productId, quantity);
        
        if (updatedRows == 0) {
            logger.warn("库存扣减失败，库存不足或商品不存在: productId={}, quantity={}", productId, quantity);
            return false;
        }
        
        logger.debug("库存扣减成功: productId={}, quantity={}, updatedRows={}", productId, quantity, updatedRows);
        checkAndLogLowStock(productId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStockWithRetry(Long productId, int quantity, int maxRetries) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在: " + productId);
        }

        checkStock(productId, quantity);

        boolean success = deductStock(productId, quantity);
        if (success) {
            return true;
        }

        int retryCount = 0;
        while (retryCount < maxRetries) {
            retryCount++;
            try {
                Thread.sleep(100L * retryCount);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("扣减库存重试时被中断: productId={}", productId);
                return false;
            }

            Product currentProduct = this.getById(productId);
            if (currentProduct != null) {
                Integer currentStock = currentProduct.getStock();
                if (currentStock == null || currentStock < quantity) {
                    logger.info("重试时发现库存不足，停止重试: productId={}, currentStock={}, requested={}",
                            productId, currentStock, quantity);
                    throw new InsufficientStockException(currentProduct.getName(), quantity, 
                            currentStock == null ? 0 : currentStock);
                }
            }

            success = deductStock(productId, quantity);
            if (success) {
                logger.info("库存扣减成功，重试次数: {}", retryCount);
                return true;
            }
        }

        logger.warn("库存扣减失败，已重试 {} 次: productId={}", maxRetries, productId);
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreStock(Long productId, int quantity) {
        if (quantity <= 0) {
            logger.warn("恢复库存数量无效: productId={}, quantity={}", productId, quantity);
            return false;
        }

        Product product = this.getById(productId);
        if (product == null) {
            logger.error("无法恢复库存：商品不存在 - productId={}, quantity={}", productId, quantity);
            return false;
        }

        int updatedRows = productMapper.restoreStock(productId, quantity);
        
        if (updatedRows > 0) {
            Integer stock = product.getStock();
            if (stock == null) {
                stock = 0;
            }
            logger.info("恢复商品库存成功: productId={}, productName={}, quantity={}, 原库存={}, 恢复后库存={}", 
                    productId, product.getName(), quantity, stock, stock + quantity);
            return true;
        } else {
            logger.error("恢复商品库存失败: productId={}, productName={}, quantity={}", 
                    productId, product.getName(), quantity);
            return false;
        }
    }

    @Override
    public List<ProductAdminVO> getLowStockProducts() {
        return productMapper.selectLowStockProducts();
    }

    @Override
    public void checkAndLogLowStock(Long productId) {
        Product product = this.getById(productId);
        if (product == null) {
            return;
        }
        Integer stock = product.getStock();
        Integer threshold = product.getLowStockThreshold();
        if (threshold == null) {
            threshold = DEFAULT_LOW_STOCK_THRESHOLD;
        }
        if (stock != null && stock < threshold) {
            logger.warn("【库存预警】商品库存低于阈值: productId={}, productName={}, currentStock={}, threshold={}",
                    product.getId(), product.getName(), stock, threshold);
        }
    }
}

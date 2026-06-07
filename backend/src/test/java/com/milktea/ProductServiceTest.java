package com.milktea;

import com.milktea.entity.Product;
import com.milktea.exception.InsufficientStockException;
import com.milktea.mapper.ProductMapper;
import com.milktea.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 测试")
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("珍珠奶茶");
        testProduct.setPrice(new BigDecimal("15.00"));
        testProduct.setStock(10);
        testProduct.setVersion(1);
    }

    @Test
    @DisplayName("测试 checkStock - 库存充足")
    void testCheckStock_SufficientStock() {
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        
        assertDoesNotThrow(() -> productService.checkStock(1L, 5));
    }

    @Test
    @DisplayName("测试 checkStock - 库存不足")
    void testCheckStock_InsufficientStock() {
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        
        InsufficientStockException exception = assertThrows(
            InsufficientStockException.class,
            () -> productService.checkStock(1L, 15)
        );
        
        assertEquals("珍珠奶茶", exception.getProductName());
        assertEquals(15, exception.getRequestedQuantity());
        assertEquals(10, exception.getAvailableStock());
    }

    @Test
    @DisplayName("测试 checkStock - 商品不存在")
    void testCheckStock_ProductNotFound() {
        when(productMapper.selectById(999L)).thenReturn(null);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> productService.checkStock(999L, 1)
        );
        
        assertEquals("商品不存在: 999", exception.getMessage());
    }

    @Test
    @DisplayName("测试 deductStock - 扣减成功")
    void testDeductStock_Success() {
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.deductStock(1L, 3)).thenReturn(1);
        
        boolean result = productService.deductStock(1L, 3);
        
        assertTrue(result);
        verify(productMapper, times(1)).deductStock(1L, 3);
    }

    @Test
    @DisplayName("测试 deductStock - 扣减失败")
    void testDeductStock_Failure() {
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.deductStock(1L, 15)).thenReturn(0);
        
        boolean result = productService.deductStock(1L, 15);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("测试 deductStock - 数量无效")
    void testDeductStock_InvalidQuantity() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> productService.deductStock(1L, 0)
        );
        
        assertEquals("扣减数量必须大于0", exception.getMessage());
    }

    @Test
    @DisplayName("测试 deductStock - 商品不存在")
    void testDeductStock_ProductNotFound() {
        when(productMapper.selectById(999L)).thenReturn(null);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> productService.deductStock(999L, 1)
        );
        
        assertEquals("商品不存在: 999", exception.getMessage());
    }

    @Test
    @DisplayName("测试 restoreStock - 恢复成功")
    void testRestoreStock_Success() {
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.restoreStock(1L, 5)).thenReturn(1);
        
        boolean result = productService.restoreStock(1L, 5);
        
        assertTrue(result);
        verify(productMapper, times(1)).restoreStock(1L, 5);
    }

    @Test
    @DisplayName("测试 restoreStock - 恢复失败")
    void testRestoreStock_Failure() {
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.restoreStock(1L, 5)).thenReturn(0);
        
        boolean result = productService.restoreStock(1L, 5);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("测试 restoreStock - 数量无效")
    void testRestoreStock_InvalidQuantity() {
        boolean result = productService.restoreStock(1L, 0);
        
        assertFalse(result);
        verify(productMapper, never()).restoreStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("测试 restoreStock - 商品不存在")
    void testRestoreStock_ProductNotFound() {
        when(productMapper.selectById(999L)).thenReturn(null);
        
        boolean result = productService.restoreStock(999L, 5);
        
        assertFalse(result);
        verify(productMapper, never()).restoreStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("测试 deductStockWithRetry - 第一次成功")
    void testDeductStockWithRetry_FirstTimeSuccess() {
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.deductStock(1L, 3)).thenReturn(1);
        
        boolean result = productService.deductStockWithRetry(1L, 3, 3);
        
        assertTrue(result);
        verify(productMapper, times(1)).deductStock(1L, 3);
    }

    @Test
    @DisplayName("测试 deductStockWithRetry - 重试后成功")
    void testDeductStockWithRetry_RetrySuccess() {
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.deductStock(1L, 3))
            .thenReturn(0)
            .thenReturn(0)
            .thenReturn(1);
        
        boolean result = productService.deductStockWithRetry(1L, 3, 3);
        
        assertTrue(result);
        verify(productMapper, times(3)).deductStock(1L, 3);
    }

    @Test
    @DisplayName("测试 deductStockWithRetry - 重试后失败")
    void testDeductStockWithRetry_RetryFailure() {
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.deductStock(1L, 3)).thenReturn(0);
        
        boolean result = productService.deductStockWithRetry(1L, 3, 2);
        
        assertFalse(result);
        verify(productMapper, times(3)).deductStock(1L, 3);
    }

    @Test
    @DisplayName("测试 deductStockWithRetry - 商品不存在")
    void testDeductStockWithRetry_ProductNotFound() {
        when(productMapper.selectById(999L)).thenReturn(null);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> productService.deductStockWithRetry(999L, 1, 3)
        );
        
        assertEquals("商品不存在: 999", exception.getMessage());
    }
}

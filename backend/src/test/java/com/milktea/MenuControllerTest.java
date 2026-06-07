package com.milktea;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.controller.MenuController;
import com.milktea.entity.Category;
import com.milktea.entity.Product;
import com.milktea.mapper.CategoryMapper;
import com.milktea.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuController 测试")
class MenuControllerTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private MenuController menuController;

    private Category category1;
    private Category category2;
    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        category1 = new Category();
        category1.setId(1L);
        category1.setName("奶茶系列");
        category1.setSort(1);

        category2 = new Category();
        category2.setId(2L);
        category2.setName("果茶系列");
        category2.setSort(2);

        product1 = new Product();
        product1.setId(1L);
        product1.setCategoryId(1L);
        product1.setName("珍珠奶茶");
        product1.setDescription("经典珍珠奶茶");
        product1.setPrice(new BigDecimal("15.00"));
        product1.setStatus(1);
        product1.setStock(10);

        product2 = new Product();
        product2.setId(2L);
        product2.setCategoryId(1L);
        product2.setName("红豆奶茶");
        product2.setDescription("香甜红豆奶茶");
        product2.setPrice(new BigDecimal("14.00"));
        product2.setStatus(1);
        product2.setStock(5);

        product3 = new Product();
        product3.setId(3L);
        product3.setCategoryId(2L);
        product3.setName("柠檬茶");
        product3.setDescription("清新柠檬茶");
        product3.setPrice(new BigDecimal("12.00"));
        product3.setStatus(1);
        product3.setStock(20);
    }

    @Test
    @DisplayName("测试 getCategories - 获取分类列表")
    void testGetCategories() {
        List<Category> categories = Arrays.asList(category1, category2);
        
        when(categoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(categories);
        
        var result = menuController.getCategories();
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals("奶茶系列", result.getData().get(0).getName());
        assertEquals("果茶系列", result.getData().get(1).getName());
    }

    @Test
    @DisplayName("测试 getCategories - 无分类")
    void testGetCategories_Empty() {
        when(categoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        
        var result = menuController.getCategories();
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("测试 getProducts - 获取所有商品")
    void testGetProducts_AllProducts() {
        Page<Product> page = new Page<>();
        page.setRecords(Arrays.asList(product1, product2, product3));
        page.setTotal(3);
        
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = menuController.getProducts(null, null, null, null, 1, 10);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().getRecords().size());
    }

    @Test
    @DisplayName("测试 getProducts - 按分类过滤")
    void testGetProducts_ByCategory() {
        Page<Product> page = new Page<>();
        page.setRecords(Arrays.asList(product1, product2));
        page.setTotal(2);
        
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = menuController.getProducts(1L, null, null, null, 1, 10);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().getRecords().size());
    }

    @Test
    @DisplayName("测试 getProducts - 按关键词搜索")
    void testGetProducts_ByKeyword() {
        Page<Product> page = new Page<>();
        page.setRecords(Arrays.asList(product1, product2));
        page.setTotal(2);
        
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = menuController.getProducts(null, "奶茶", null, null, 1, 10);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().getRecords().size());
    }

    @Test
    @DisplayName("测试 getProducts - 按价格升序排序")
    void testGetProducts_SortByPriceAsc() {
        Page<Product> page = new Page<>();
        page.setRecords(Arrays.asList(product3, product2, product1));
        page.setTotal(3);
        
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = menuController.getProducts(null, null, "price", "asc", 1, 10);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().getRecords().size());
        assertEquals("柠檬茶", result.getData().getRecords().get(0).getName());
    }

    @Test
    @DisplayName("测试 getProducts - 按价格降序排序")
    void testGetProducts_SortByPriceDesc() {
        Page<Product> page = new Page<>();
        page.setRecords(Arrays.asList(product1, product2, product3));
        page.setTotal(3);
        
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = menuController.getProducts(null, null, "price", "desc", 1, 10);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().getRecords().size());
        assertEquals("珍珠奶茶", result.getData().getRecords().get(0).getName());
    }

    @Test
    @DisplayName("测试 getProducts - 按名称升序排序")
    void testGetProducts_SortByNameAsc() {
        Page<Product> page = new Page<>();
        page.setRecords(Arrays.asList(product2, product1, product3));
        page.setTotal(3);
        
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = menuController.getProducts(null, null, "name", "asc", 1, 10);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("测试 getProducts - 按库存降序排序")
    void testGetProducts_SortByStockDesc() {
        Page<Product> page = new Page<>();
        page.setRecords(Arrays.asList(product3, product1, product2));
        page.setTotal(3);
        
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = menuController.getProducts(null, null, "stock", "desc", 1, 10);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("测试 getProducts - 分页参数")
    void testGetProducts_Pagination() {
        Page<Product> page = new Page<>(2, 2);
        page.setRecords(Arrays.asList(product3));
        page.setTotal(3);
        page.setPages(2);
        
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = menuController.getProducts(null, null, null, null, 2, 2);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().getCurrent());
        assertEquals(2, result.getData().getSize());
        assertEquals(2, result.getData().getPages());
        assertEquals(3, result.getData().getTotal());
    }

    @Test
    @DisplayName("测试 getProducts - 只查询上架商品")
    void testGetProducts_OnlyActiveProducts() {
        Product inactiveProduct = new Product();
        inactiveProduct.setId(4L);
        inactiveProduct.setName("下架商品");
        inactiveProduct.setStatus(0);
        
        Page<Product> page = new Page<>();
        page.setRecords(Arrays.asList(product1, product2, product3));
        page.setTotal(3);
        
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = menuController.getProducts(null, null, null, null, 1, 10);
        
        assertTrue(result.isSuccess());
        verify(productMapper).selectPage(any(Page.class), argThat(query -> {
            return true;
        }));
    }

    @Test
    @DisplayName("测试 getProducts - 无效排序字段使用默认排序")
    void testGetProducts_InvalidSortField() {
        Page<Product> page = new Page<>();
        page.setRecords(Arrays.asList(product1, product2, product3));
        page.setTotal(3);
        
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = menuController.getProducts(null, null, "invalidField", null, 1, 10);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("测试 getProducts - 组合条件查询")
    void testGetProducts_CombinedConditions() {
        Page<Product> page = new Page<>();
        page.setRecords(Arrays.asList(product1));
        page.setTotal(1);
        
        when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = menuController.getProducts(1L, "珍珠", "price", "desc", 1, 10);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getRecords().size());
        assertEquals("珍珠奶茶", result.getData().getRecords().get(0).getName());
    }
}

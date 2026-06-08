package com.milktea;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.milktea.controller.CartController;
import com.milktea.dto.CartGroupVO;
import com.milktea.entity.CartItem;
import com.milktea.entity.Product;
import com.milktea.exception.InsufficientStockException;
import com.milktea.mapper.CartItemMapper;
import com.milktea.mapper.ProductMapper;
import com.milktea.service.ProductService;
import com.milktea.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartController 测试")
class CartControllerTest {

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CartController cartController;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User testUser;
    private Product testProduct;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("珍珠奶茶");
        testProduct.setPrice(new BigDecimal("15.00"));
        testProduct.setStock(10);
        testProduct.setSpecPriceRules("{\"size\":{\"大杯\":3},\"topping\":{\"珍珠\":2,\"布丁\":2}}");

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setUserId(1L);
        testCartItem.setProductId(1L);
        testCartItem.setQuantity(2);
        testCartItem.setSpecs("{\"size\":\"中杯\",\"temp\":\"少冰\",\"sugar\":\"全糖\",\"topping\":[]}");
        testCartItem.setUnitPrice(new BigDecimal("15.00"));

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getDetails()).thenReturn(1L);
    }

    @Test
    @DisplayName("测试 getCart - 获取购物车列表")
    void testGetCart() {
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(testCartItem);
        
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(cartItems);
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        
        var result = cartController.getCart();
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(1L, result.getData().get(0).getProductId());
        assertEquals("珍珠奶茶", result.getData().get(0).getProductName());
        assertEquals(1, result.getData().get(0).getSpecs().size());
    }

    @Test
    @DisplayName("测试 getCart - 购物车为空")
    void testGetCart_Empty() {
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        
        var result = cartController.getCart();
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("测试 getCart - 同商品不同规格合并为一个分组")
    void testGetCart_SameProductDifferentSpecsGrouped() {
        CartItem item1 = new CartItem();
        item1.setId(1L);
        item1.setUserId(1L);
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setSpecs("{\"size\":\"中杯\",\"temp\":\"少冰\",\"sugar\":\"全糖\",\"topping\":[]}");
        item1.setUnitPrice(new BigDecimal("15.00"));

        CartItem item2 = new CartItem();
        item2.setId(2L);
        item2.setUserId(1L);
        item2.setProductId(1L);
        item2.setQuantity(1);
        item2.setSpecs("{\"size\":\"大杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[\"珍珠\"]}");
        item2.setUnitPrice(new BigDecimal("20.00"));

        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(item1);
        cartItems.add(item2);

        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(cartItems);
        when(productMapper.selectById(1L)).thenReturn(testProduct);

        var result = cartController.getCart();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        CartGroupVO group = result.getData().get(0);
        assertEquals(1L, group.getProductId());
        assertEquals("珍珠奶茶", group.getProductName());
        assertEquals(2, group.getSpecs().size());
    }

    @Test
    @DisplayName("测试 addToCart - 商品不存在")
    void testAddToCart_ProductNotFound() {
        CartItem newCartItem = new CartItem();
        newCartItem.setProductId(999L);
        newCartItem.setQuantity(1);
        newCartItem.setSpecs("{\"size\":\"中杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[]}");
        
        when(productMapper.selectById(999L)).thenReturn(null);
        
        var result = cartController.addToCart(newCartItem);
        
        assertFalse(result.isSuccess());
        assertEquals("Product not found", result.getMessage());
        verify(cartItemMapper, never()).insert(any(CartItem.class));
    }

    @Test
    @DisplayName("测试 addToCart - 新增购物车项成功")
    void testAddToCart_AddNewItemSuccess() {
        CartItem newCartItem = new CartItem();
        newCartItem.setProductId(1L);
        newCartItem.setQuantity(1);
        newCartItem.setSpecs("{\"size\":\"中杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[]}");
        newCartItem.setUnitPrice(new BigDecimal("15.00"));
        
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productService.calculateUnitPrice(eq(testProduct), eq("{\"size\":\"中杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[]}")))
                .thenReturn(new BigDecimal("15.00"));
        when(cartItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doNothing().when(productService).checkStock(eq(1L), eq(1));
        when(cartItemMapper.insert(any(CartItem.class))).thenReturn(1);
        
        var result = cartController.addToCart(newCartItem);
        
        assertTrue(result.isSuccess());
        assertEquals("Added to cart", result.getMessage());
        verify(cartItemMapper, times(1)).insert(any(CartItem.class));
    }

    @Test
    @DisplayName("测试 addToCart - 更新已存在的购物车项成功")
    void testAddToCart_UpdateExistingItemSuccess() {
        CartItem existingCartItem = new CartItem();
        existingCartItem.setId(1L);
        existingCartItem.setUserId(1L);
        existingCartItem.setProductId(1L);
        existingCartItem.setQuantity(2);
        existingCartItem.setSpecs("{\"size\":\"中杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[]}");
        existingCartItem.setUnitPrice(new BigDecimal("15.00"));
        
        CartItem newCartItem = new CartItem();
        newCartItem.setProductId(1L);
        newCartItem.setQuantity(3);
        newCartItem.setSpecs("{\"size\":\"中杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[]}");
        newCartItem.setUnitPrice(new BigDecimal("15.00"));
        
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productService.calculateUnitPrice(eq(testProduct), eq("{\"size\":\"中杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[]}")))
                .thenReturn(new BigDecimal("15.00"));
        when(cartItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingCartItem);
        doNothing().when(productService).checkStock(eq(1L), eq(5));
        when(cartItemMapper.updateById(any(CartItem.class))).thenReturn(1);
        
        var result = cartController.addToCart(newCartItem);
        
        assertTrue(result.isSuccess());
        assertEquals("Added to cart", result.getMessage());
        verify(cartItemMapper, times(1)).updateById(any(CartItem.class));
    }

    @Test
    @DisplayName("测试 addToCart - 库存不足")
    void testAddToCart_InsufficientStock() {
        CartItem newCartItem = new CartItem();
        newCartItem.setProductId(1L);
        newCartItem.setQuantity(15);
        newCartItem.setSpecs("{\"size\":\"大杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[\"珍珠\"]}");
        newCartItem.setUnitPrice(new BigDecimal("20.00"));
        
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productService.calculateUnitPrice(eq(testProduct), eq("{\"size\":\"大杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[\"珍珠\"]}")))
                .thenReturn(new BigDecimal("20.00"));
        when(cartItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doThrow(new InsufficientStockException("珍珠奶茶", 15, 10))
            .when(productService).checkStock(eq(1L), eq(15));
        
        var result = cartController.addToCart(newCartItem);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("库存不足"));
    }

    @Test
    @DisplayName("测试 updateQuantity - 数量无效")
    void testUpdateQuantity_InvalidQuantity() {
        var result = cartController.updateQuantity(1L, 0);
        
        assertFalse(result.isSuccess());
        assertEquals("Quantity must be greater than 0", result.getMessage());
        verify(cartItemMapper, never()).updateById(any(CartItem.class));
    }

    @Test
    @DisplayName("测试 updateQuantity - 购物车项不存在")
    void testUpdateQuantity_CartItemNotFound() {
        when(cartItemMapper.selectById(999L)).thenReturn(null);
        
        var result = cartController.updateQuantity(999L, 5);
        
        assertFalse(result.isSuccess());
        assertEquals("Cart item not found", result.getMessage());
    }

    @Test
    @DisplayName("测试 updateQuantity - 无权操作")
    void testUpdateQuantity_NotAuthorized() {
        CartItem otherUserCartItem = new CartItem();
        otherUserCartItem.setId(2L);
        otherUserCartItem.setUserId(999L);
        otherUserCartItem.setProductId(1L);
        otherUserCartItem.setQuantity(2);
        
        when(cartItemMapper.selectById(2L)).thenReturn(otherUserCartItem);
        
        var result = cartController.updateQuantity(2L, 5);
        
        assertFalse(result.isSuccess());
        assertEquals("Not authorized to update this cart item", result.getMessage());
    }

    @Test
    @DisplayName("测试 updateQuantity - 更新成功")
    void testUpdateQuantity_Success() {
        when(cartItemMapper.selectById(1L)).thenReturn(testCartItem);
        doNothing().when(productService).checkStock(eq(1L), eq(5));
        when(cartItemMapper.updateById(any(CartItem.class))).thenReturn(1);
        
        var result = cartController.updateQuantity(1L, 5);
        
        assertTrue(result.isSuccess());
        assertEquals("Updated", result.getMessage());
        verify(cartItemMapper, times(1)).updateById(any(CartItem.class));
    }

    @Test
    @DisplayName("测试 updateQuantity - 库存不足")
    void testUpdateQuantity_InsufficientStock() {
        when(cartItemMapper.selectById(1L)).thenReturn(testCartItem);
        doThrow(new InsufficientStockException("珍珠奶茶", 15, 10))
            .when(productService).checkStock(eq(1L), eq(15));
        
        var result = cartController.updateQuantity(1L, 15);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("库存不足"));
    }

    @Test
    @DisplayName("测试 removeFromCart - 购物车项不存在")
    void testRemoveFromCart_CartItemNotFound() {
        when(cartItemMapper.selectById(999L)).thenReturn(null);
        
        var result = cartController.removeFromCart(999L);
        
        assertFalse(result.isSuccess());
        assertEquals("Cart item not found", result.getMessage());
    }

    @Test
    @DisplayName("测试 removeFromCart - 无权操作")
    void testRemoveFromCart_NotAuthorized() {
        CartItem otherUserCartItem = new CartItem();
        otherUserCartItem.setId(2L);
        otherUserCartItem.setUserId(999L);
        
        when(cartItemMapper.selectById(2L)).thenReturn(otherUserCartItem);
        
        var result = cartController.removeFromCart(2L);
        
        assertFalse(result.isSuccess());
        assertEquals("Not authorized to delete this cart item", result.getMessage());
    }

    @Test
    @DisplayName("测试 removeFromCart - 删除成功")
    void testRemoveFromCart_Success() {
        when(cartItemMapper.selectById(1L)).thenReturn(testCartItem);
        when(cartItemMapper.deleteById(1L)).thenReturn(1);
        
        var result = cartController.removeFromCart(1L);
        
        assertTrue(result.isSuccess());
        assertEquals("Removed", result.getMessage());
        verify(cartItemMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("测试 clearCart - 清空购物车")
    void testClearCart() {
        when(cartItemMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);
        
        var result = cartController.clearCart();
        
        assertTrue(result.isSuccess());
        assertEquals("Cleared", result.getMessage());
        verify(cartItemMapper, times(1)).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("测试 addToCart - 不同规格视为不同商品")
    void testAddToCart_DifferentSpecs() {
        CartItem existingCartItem = new CartItem();
        existingCartItem.setId(1L);
        existingCartItem.setUserId(1L);
        existingCartItem.setProductId(1L);
        existingCartItem.setQuantity(2);
        existingCartItem.setSpecs("{\"size\":\"中杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[]}");
        existingCartItem.setUnitPrice(new BigDecimal("15.00"));
        
        CartItem newCartItem = new CartItem();
        newCartItem.setProductId(1L);
        newCartItem.setQuantity(1);
        newCartItem.setSpecs("{\"size\":\"大杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[\"珍珠\"]}");
        newCartItem.setUnitPrice(new BigDecimal("20.00"));
        
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productService.calculateUnitPrice(eq(testProduct), eq("{\"size\":\"大杯\",\"temp\":\"常规冰\",\"sugar\":\"全糖\",\"topping\":[\"珍珠\"]}")))
                .thenReturn(new BigDecimal("20.00"));
        when(cartItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doNothing().when(productService).checkStock(eq(1L), eq(1));
        when(cartItemMapper.insert(any(CartItem.class))).thenReturn(1);
        
        var result = cartController.addToCart(newCartItem);
        
        assertTrue(result.isSuccess());
        verify(cartItemMapper, times(1)).insert(any(CartItem.class));
    }
}

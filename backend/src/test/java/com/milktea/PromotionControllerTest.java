package com.milktea;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.milktea.controller.PromotionController;
import com.milktea.dto.PromotionCalculateRequest;
import com.milktea.entity.CartItem;
import com.milktea.entity.Product;
import com.milktea.entity.Promotion;
import com.milktea.mapper.CartItemMapper;
import com.milktea.mapper.ProductMapper;
import com.milktea.service.PromotionService;
import com.milktea.service.UserService;
import com.milktea.util.PromotionCalculator;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionController 测试")
class PromotionControllerTest {

    @Mock
    private PromotionService promotionService;

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private PromotionController promotionController;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getDetails()).thenReturn(1L);
    }

    @Test
    @DisplayName("测试 calculatePromotion - 购物车为空时返回零优惠")
    void testCalculatePromotion_EmptyCart() {
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        PromotionCalculateRequest request = new PromotionCalculateRequest();
        request.setOrderAmount(new BigDecimal("88.00"));
        var result = promotionController.calculatePromotion(request);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(new BigDecimal("0"), result.getData().getOrderAmount());
        assertEquals(new BigDecimal("0"), result.getData().getDiscountAmount());
        assertEquals(new BigDecimal("0"), result.getData().getFinalAmount());
        assertFalse(result.getData().isApplied());
        verify(promotionService, never()).calculateBestPromotion(any());
    }

    @Test
    @DisplayName("测试 calculatePromotion - 返回稳定的促销计算结果")
    void testCalculatePromotion_AppliedPromotion() {
        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setUserId(1L);
        cartItem.setProductId(1L);
        cartItem.setQuantity(2);
        cartItem.setUnitPrice(new BigDecimal("15.00"));

        Product product = new Product();
        product.setId(1L);
        product.setCategoryId(9L);

        Promotion promotion = new Promotion();
        promotion.setId(101L);
        promotion.setName("满30减5");

        PromotionCalculator.PromotionResult promotionResult = PromotionCalculator.PromotionResult.of(
                promotion,
                new BigDecimal("5.00")
        );

        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(cartItem));
        when(productMapper.selectById(1L)).thenReturn(product);
        when(promotionService.calculateBestPromotion(any())).thenReturn(promotionResult);

        PromotionCalculateRequest request = new PromotionCalculateRequest();
        request.setOrderAmount(new BigDecimal("30.00"));
        var result = promotionController.calculatePromotion(request);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(new BigDecimal("30.00"), result.getData().getOrderAmount());
        assertEquals(new BigDecimal("5.00"), result.getData().getDiscountAmount());
        assertEquals(new BigDecimal("25.00"), result.getData().getFinalAmount());
        assertTrue(result.getData().isApplied());
        assertEquals(101L, result.getData().getPromotionId());
        assertEquals("满30减5", result.getData().getPromotionName());
        verify(promotionService).calculateBestPromotion(any());
    }
}

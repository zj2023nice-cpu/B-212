package com.milktea;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.milktea.controller.FeedbackController;
import com.milktea.entity.Feedback;
import com.milktea.entity.Order;
import com.milktea.mapper.FeedbackMapper;
import com.milktea.mapper.OrderMapper;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackController 测试")
class FeedbackControllerTest {

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private FeedbackController feedbackController;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User testUser;
    private Order testOrder;
    private Feedback testFeedback;
    private Feedback testFeedback2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(1L);
        testOrder.setOrderSn("TEST123");
        testOrder.setTotalAmount(new BigDecimal("30.00"));
        testOrder.setStatus(4);

        testFeedback = new Feedback();
        testFeedback.setId(1L);
        testFeedback.setOrderId(1L);
        testFeedback.setUserId(1L);
        testFeedback.setRating(5);
        testFeedback.setContent("非常好喝！");

        testFeedback2 = new Feedback();
        testFeedback2.setId(2L);
        testFeedback2.setOrderId(2L);
        testFeedback2.setUserId(2L);
        testFeedback2.setRating(4);
        testFeedback2.setContent("还不错");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getDetails()).thenReturn(1L);
    }

    @Test
    @DisplayName("测试 submitFeedback - 订单不存在")
    void testSubmitFeedback_OrderNotFound() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(999L);
        feedback.setRating(5);
        feedback.setContent("测试评价");
        
        when(orderMapper.selectById(999L)).thenReturn(null);
        
        var result = feedbackController.submitFeedback(feedback);
        
        assertFalse(result.isSuccess());
        assertEquals("Order not found", result.getMessage());
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    @Test
    @DisplayName("测试 submitFeedback - 无权评价他人订单")
    void testSubmitFeedback_NotAuthorized() {
        Order otherUserOrder = new Order();
        otherUserOrder.setId(2L);
        otherUserOrder.setUserId(999L);
        
        Feedback feedback = new Feedback();
        feedback.setOrderId(2L);
        feedback.setRating(5);
        feedback.setContent("测试评价");
        
        when(orderMapper.selectById(2L)).thenReturn(otherUserOrder);
        
        var result = feedbackController.submitFeedback(feedback);
        
        assertFalse(result.isSuccess());
        assertEquals("Not authorized to submit feedback for this order", result.getMessage());
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    @Test
    @DisplayName("测试 submitFeedback - 提交评价成功")
    void testSubmitFeedback_Success() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setRating(5);
        feedback.setContent("非常好喝！");
        
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        
        var result = feedbackController.submitFeedback(feedback);
        
        assertTrue(result.isSuccess());
        assertEquals("Feedback submitted", result.getMessage());
        verify(feedbackMapper, times(1)).insert(any(Feedback.class));
        verify(orderMapper, times(1)).updateById(any(Order.class));
    }

    @Test
    @DisplayName("测试 submitFeedback - 验证订单状态更新为已评价")
    void testSubmitFeedback_OrderStatusUpdated() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setRating(5);
        feedback.setContent("非常好喝！");
        
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);
        
        feedbackController.submitFeedback(feedback);
        
        verify(orderMapper).updateById(argThat(order -> 
            order.getStatus() == 5
        ));
    }

    @Test
    @DisplayName("测试 submitFeedback - 验证userId设置")
    void testSubmitFeedback_UserIdSet() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setRating(5);
        feedback.setContent("非常好喝！");
        
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        
        feedbackController.submitFeedback(feedback);
        
        verify(feedbackMapper).insert(argThat(fb -> 
            fb.getUserId() != null && fb.getUserId().equals(1L)
        ));
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 获取商品评价列表")
    void testGetProductFeedbacks() {
        List<Feedback> feedbacks = Arrays.asList(testFeedback, testFeedback2);
        
        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(feedbacks);
        
        var result = feedbackController.getProductFeedbacks(1L);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals("非常好喝！", result.getData().get(0).getContent());
        assertEquals("还不错", result.getData().get(1).getContent());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 无评价")
    void testGetProductFeedbacks_Empty() {
        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        
        var result = feedbackController.getProductFeedbacks(999L);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("测试 submitFeedback - 使用旧Token格式（通过用户名查询）")
    void testSubmitFeedback_WithOldTokenFormat() {
        when(authentication.getDetails()).thenReturn("notALongType");
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getByUsername("testuser")).thenReturn(testUser);
        
        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setRating(5);
        feedback.setContent("测试评价");
        
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        
        var result = feedbackController.submitFeedback(feedback);
        
        assertTrue(result.isSuccess());
        verify(userService, times(1)).getByUsername("testuser");
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 按创建时间降序排序")
    void testGetProductFeedbacks_OrderedByCreateTime() {
        List<Feedback> feedbacks = Arrays.asList(testFeedback2, testFeedback);
        
        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(feedbacks);
        
        var result = feedbackController.getProductFeedbacks(1L);
        
        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
        assertEquals("还不错", result.getData().get(0).getContent());
        assertEquals("非常好喝！", result.getData().get(1).getContent());
    }

    @Test
    @DisplayName("测试 submitFeedback - 完整流程验证")
    void testSubmitFeedback_FullFlow() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setRating(5);
        feedback.setContent("珍珠奶茶非常好喝，珍珠Q弹有嚼劲！");
        feedback.setImages("image1.jpg,image2.jpg");
        
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.insert(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback insertedFeedback = invocation.getArgument(0);
            insertedFeedback.setId(3L);
            return 1;
        });
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        
        var result = feedbackController.submitFeedback(feedback);
        
        assertTrue(result.isSuccess());
        
        verify(feedbackMapper).insert(argThat(fb -> 
            fb.getOrderId().equals(1L) &&
            fb.getUserId().equals(1L) &&
            fb.getRating() == 5 &&
            fb.getContent().equals("珍珠奶茶非常好喝，珍珠Q弹有嚼劲！") &&
            fb.getImages().equals("image1.jpg,image2.jpg")
        ));
        
        verify(orderMapper).updateById(argThat(order -> 
            order.getId().equals(1L) &&
            order.getStatus() == 5
        ));
    }
}

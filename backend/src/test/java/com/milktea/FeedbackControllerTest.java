package com.milktea;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.milktea.controller.FeedbackController;
import com.milktea.entity.Feedback;
import com.milktea.entity.Order;
import com.milktea.entity.User;
import com.milktea.enums.OrderStatus;
import com.milktea.mapper.FeedbackMapper;
import com.milktea.mapper.OrderItemMapper;
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
import java.util.*;

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
    private OrderItemMapper orderItemMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private FeedbackController feedbackController;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private Order testOrder;
    private Feedback testFeedback;
    private Feedback testFeedback2;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(1L);
        testOrder.setOrderSn("TEST123");
        testOrder.setTotalAmount(new BigDecimal("30.00"));
        testOrder.setStatus(OrderStatus.COMPLETED);

        testFeedback = new Feedback();
        testFeedback.setId(1L);
        testFeedback.setOrderId(1L);
        testFeedback.setUserId(1L);
        testFeedback.setProductId(1L);
        testFeedback.setRating(5);
        testFeedback.setContent("非常好喝！");

        testFeedback2 = new Feedback();
        testFeedback2.setId(2L);
        testFeedback2.setOrderId(2L);
        testFeedback2.setUserId(2L);
        testFeedback2.setProductId(1L);
        testFeedback2.setRating(4);
        testFeedback2.setContent("还不错");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getDetails()).thenReturn(1L);
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 订单不存在")
    void testSubmitFeedbacks_OrderNotFound() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(999L);
        feedback.setProductId(1L);
        feedback.setRating(5);
        feedback.setContent("测试评价");

        when(orderMapper.selectById(999L)).thenReturn(null);

        var result = feedbackController.submitFeedbacks(Arrays.asList(feedback));

        assertFalse(result.isSuccess());
        assertEquals("Order not found", result.getMessage());
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 无权评价他人订单")
    void testSubmitFeedbacks_NotAuthorized() {
        Order otherUserOrder = new Order();
        otherUserOrder.setId(2L);
        otherUserOrder.setUserId(999L);
        otherUserOrder.setStatus(OrderStatus.COMPLETED);

        Feedback feedback = new Feedback();
        feedback.setOrderId(2L);
        feedback.setProductId(1L);
        feedback.setRating(5);
        feedback.setContent("测试评价");

        when(orderMapper.selectById(2L)).thenReturn(otherUserOrder);

        var result = feedbackController.submitFeedbacks(Arrays.asList(feedback));

        assertFalse(result.isSuccess());
        assertEquals("Not authorized to submit feedback for this order", result.getMessage());
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 提交评价成功")
    void testSubmitFeedbacks_Success() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setProductId(1L);
        feedback.setRating(5);
        feedback.setContent("非常好喝！");

        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        var result = feedbackController.submitFeedbacks(Arrays.asList(feedback));

        assertTrue(result.isSuccess());
        assertEquals("Feedback submitted", result.getMessage());
        verify(feedbackMapper, times(1)).insert(any(Feedback.class));
        verify(orderMapper, times(1)).updateById(any(Order.class));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 订单状态不是已送达")
    void testSubmitFeedbacks_OrderNotCompleted() {
        Order pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setUserId(1L);
        pendingOrder.setStatus(OrderStatus.DELIVERING);

        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setProductId(1L);
        feedback.setRating(5);
        feedback.setContent("测试评价");

        when(orderMapper.selectById(1L)).thenReturn(pendingOrder);

        var result = feedbackController.submitFeedbacks(Arrays.asList(feedback));

        assertFalse(result.isSuccess());
        assertEquals("Only completed orders can be reviewed", result.getMessage());
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 重复评价")
    void testSubmitFeedbacks_DuplicateReview() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setProductId(1L);
        feedback.setRating(5);
        feedback.setContent("测试评价");

        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        var result = feedbackController.submitFeedbacks(Arrays.asList(feedback));

        assertFalse(result.isSuccess());
        assertEquals("该订单已评价，不能重复评价", result.getMessage());
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 验证订单状态更新为已评价")
    void testSubmitFeedbacks_OrderStatusUpdated() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setProductId(1L);
        feedback.setRating(5);
        feedback.setContent("非常好喝！");

        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);

        feedbackController.submitFeedbacks(Arrays.asList(feedback));

        verify(orderMapper).updateById(argThat(order ->
            order.getStatus() == OrderStatus.REVIEWED
        ));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 验证userId设置")
    void testSubmitFeedbacks_UserIdSet() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setProductId(1L);
        feedback.setRating(5);
        feedback.setContent("非常好喝！");

        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        feedbackController.submitFeedbacks(Arrays.asList(feedback));

        verify(feedbackMapper).insert(argThat(fb ->
            fb.getUserId() != null && fb.getUserId().equals(1L)
        ));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 评分超出范围")
    void testSubmitFeedbacks_RatingOutOfRange() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setProductId(1L);
        feedback.setRating(0);
        feedback.setContent("测试评价");

        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        var result = feedbackController.submitFeedbacks(Arrays.asList(feedback));

        assertFalse(result.isSuccess());
        assertEquals("评分必须在1-5之间", result.getMessage());
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 商品ID为空")
    void testSubmitFeedbacks_ProductIdNull() {
        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setRating(5);
        feedback.setContent("测试评价");

        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        var result = feedbackController.submitFeedbacks(Arrays.asList(feedback));

        assertFalse(result.isSuccess());
        assertEquals("商品ID不能为空", result.getMessage());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 获取商品评价列表")
    void testGetProductFeedbacks() {
        com.milktea.entity.User user1 = new com.milktea.entity.User();
        user1.setId(1L);
        user1.setNickname("用户1");
        com.milktea.entity.User user2 = new com.milktea.entity.User();
        user2.setId(2L);
        user2.setNickname("用户2");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(testFeedback, testFeedback2));
        when(userService.listByIds(any(Collection.class))).thenReturn(Arrays.asList(user1, user2));

        var result = feedbackController.getProductFeedbacks(1L, null, null, "desc");

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals("用户1", result.getData().get(0).getNickname());
        assertEquals("用户2", result.getData().get(1).getNickname());
        verify(userService, times(1)).listByIds(any(Collection.class));
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 无评价")
    void testGetProductFeedbacks_Empty() {
        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        var result = feedbackController.getProductFeedbacks(999L, null, null, "desc");

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
        verify(userService, never()).listByIds(any(Collection.class));
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 按评分筛选")
    void testGetProductFeedbacks_FilterByRating() {
        com.milktea.entity.User user1 = new com.milktea.entity.User();
        user1.setId(1L);
        user1.setNickname("用户1");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(testFeedback));
        when(userService.listByIds(any(Collection.class))).thenReturn(Arrays.asList(user1));

        var result = feedbackController.getProductFeedbacks(1L, 5, null, "desc");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals(5, result.getData().get(0).getRating());
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 使用旧Token格式（通过用户名查询）")
    void testSubmitFeedbacks_WithOldTokenFormat() {
        when(authentication.getDetails()).thenReturn("notALongType");
        when(authentication.getName()).thenReturn("testuser");
        com.milktea.entity.User testUser = new com.milktea.entity.User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        when(userService.getByUsername("testuser")).thenReturn(testUser);

        Feedback feedback = new Feedback();
        feedback.setOrderId(1L);
        feedback.setProductId(1L);
        feedback.setRating(5);
        feedback.setContent("测试评价");

        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        var result = feedbackController.submitFeedbacks(Arrays.asList(feedback));

        assertTrue(result.isSuccess());
        verify(userService, times(1)).getByUsername("testuser");
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 完整流程验证（多商品评价）")
    void testSubmitFeedbacks_FullFlow() {
        Feedback feedback1 = new Feedback();
        feedback1.setOrderId(1L);
        feedback1.setProductId(1L);
        feedback1.setRating(5);
        feedback1.setContent("珍珠奶茶非常好喝！");
        feedback1.setImages("/uploads/img1.jpg,/uploads/img2.jpg");

        Feedback feedback2 = new Feedback();
        feedback2.setOrderId(1L);
        feedback2.setProductId(2L);
        feedback2.setRating(4);
        feedback2.setContent("波波烤奶也不错");

        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(feedbackMapper.insert(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback insertedFeedback = invocation.getArgument(0);
            insertedFeedback.setId(3L);
            return 1;
        });
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        var result = feedbackController.submitFeedbacks(Arrays.asList(feedback1, feedback2));

        assertTrue(result.isSuccess());
        verify(feedbackMapper, times(2)).insert(any(Feedback.class));

        verify(orderMapper).updateById(argThat(order ->
            order.getId().equals(1L) &&
            order.getStatus() == OrderStatus.REVIEWED
        ));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 空评价列表")
    void testSubmitFeedbacks_EmptyList() {
        var result = feedbackController.submitFeedbacks(new ArrayList<>());

        assertFalse(result.isSuccess());
        assertEquals("评价列表不能为空", result.getMessage());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 批量查询用户信息避免N+1")
    void testGetProductFeedbacks_BatchUserQuery() {
        com.milktea.entity.User user1 = new com.milktea.entity.User();
        user1.setId(1L);
        user1.setNickname("用户A");
        user1.setAvatarUrl("/avatar/a.png");
        com.milktea.entity.User user2 = new com.milktea.entity.User();
        user2.setId(2L);
        user2.setNickname("用户B");
        user2.setAvatarUrl("/avatar/b.png");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(testFeedback, testFeedback2));
        when(userService.listByIds(any(Collection.class))).thenReturn(Arrays.asList(user1, user2));

        feedbackController.getProductFeedbacks(1L, null, null, "desc");

        verify(userService, times(1)).listByIds(any(Collection.class));
        verify(userService, never()).getById(anyLong());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 用户不存在时显示匿名")
    void testGetProductFeedbacks_UserNotFound() {
        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(testFeedback));
        when(userService.listByIds(any(Collection.class))).thenReturn(new ArrayList<>());

        var result = feedbackController.getProductFeedbacks(1L, null, null, "desc");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("匿名用户", result.getData().get(0).getNickname());
    }

    @Test
    @DisplayName("测试 replyFeedback - 管理员回复成功")
    void testReplyFeedback_Success() {
        testFeedback.setId(1L);
        when(feedbackMapper.selectById(1L)).thenReturn(testFeedback);
        when(feedbackMapper.updateById(any(Feedback.class))).thenReturn(1);

        Map<String, String> body = new HashMap<>();
        body.put("reply", "感谢您的评价！");
        var result = feedbackController.replyFeedback(1L, body);

        assertTrue(result.isSuccess());
        assertEquals("回复成功", result.getMessage());
        verify(feedbackMapper).updateById(argThat(fb ->
            fb.getAdminReply() != null && fb.getAdminReply().equals("感谢您的评价！")
        ));
    }

    @Test
    @DisplayName("测试 replyFeedback - 评价不存在")
    void testReplyFeedback_FeedbackNotFound() {
        when(feedbackMapper.selectById(999L)).thenReturn(null);

        Map<String, String> body = new HashMap<>();
        body.put("reply", "回复内容");
        var result = feedbackController.replyFeedback(999L, body);

        assertFalse(result.isSuccess());
        assertEquals("评价不存在", result.getMessage());
        verify(feedbackMapper, never()).updateById(any(Feedback.class));
    }

    @Test
    @DisplayName("测试 replyFeedback - 回复内容为空")
    void testReplyFeedback_EmptyReply() {
        Map<String, String> body = new HashMap<>();
        body.put("reply", "");
        var result = feedbackController.replyFeedback(1L, body);

        assertFalse(result.isSuccess());
        assertEquals("回复内容不能为空", result.getMessage());
        verify(feedbackMapper, never()).updateById(any(Feedback.class));
    }

    @Test
    @DisplayName("测试 replyFeedback - 回复内容为null")
    void testReplyFeedback_NullReply() {
        Map<String, String> body = new HashMap<>();
        var result = feedbackController.replyFeedback(1L, body);

        assertFalse(result.isSuccess());
        assertEquals("回复内容不能为空", result.getMessage());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 按有图筛选")
    void testGetProductFeedbacks_FilterByHasImage() {
        testFeedback.setImages("/uploads/img1.jpg,/uploads/img2.jpg");
        com.milktea.entity.User user1 = new com.milktea.entity.User();
        user1.setId(1L);
        user1.setNickname("用户1");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(testFeedback));
        when(userService.listByIds(any(Collection.class))).thenReturn(Arrays.asList(user1));

        var result = feedbackController.getProductFeedbacks(1L, null, true, "desc");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertFalse(result.getData().get(0).getImages().isEmpty());
    }

    @Test
    @DisplayName("测试 convertToVOList - 包含adminReply")
    void testConvertToVOList_WithAdminReply() {
        testFeedback.setAdminReply("感谢您的评价！");
        com.milktea.entity.User user1 = new com.milktea.entity.User();
        user1.setId(1L);
        user1.setNickname("用户1");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(testFeedback));
        when(userService.listByIds(any(Collection.class))).thenReturn(Arrays.asList(user1));

        var result = feedbackController.getProductFeedbacks(1L, null, null, "desc");

        assertTrue(result.isSuccess());
        assertEquals("感谢您的评价！", result.getData().get(0).getAdminReply());
    }
}

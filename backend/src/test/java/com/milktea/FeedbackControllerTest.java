package com.milktea;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.milktea.controller.FeedbackController;
import com.milktea.dto.FeedbackSubmitDTO;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        FeedbackSubmitDTO feedback = buildFeedbackDTO(999L, 1L, 5, "测试评价", List.of());
        when(orderMapper.selectById(999L)).thenReturn(null);

        var result = feedbackController.submitFeedbacks(List.of(feedback));

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

        FeedbackSubmitDTO feedback = buildFeedbackDTO(2L, 1L, 5, "测试评价", List.of());
        when(orderMapper.selectById(2L)).thenReturn(otherUserOrder);

        var result = feedbackController.submitFeedbacks(List.of(feedback));

        assertFalse(result.isSuccess());
        assertEquals("Not authorized to submit feedback for this order", result.getMessage());
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 提交评价成功并序列化图片数组")
    void testSubmitFeedbacks_Success() {
        FeedbackSubmitDTO feedback = buildFeedbackDTO(
                1L,
                1L,
                5,
                "非常好喝！",
                Arrays.asList("/uploads/img1.jpg", " /uploads/img2.jpg ", "", null)
        );

        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        var result = feedbackController.submitFeedbacks(List.of(feedback));

        assertTrue(result.isSuccess());
        assertEquals("Feedback submitted", result.getMessage());
        verify(feedbackMapper).insert(argThat(saved ->
                saved.getOrderId().equals(1L)
                        && saved.getUserId().equals(1L)
                        && saved.getProductId().equals(1L)
                        && "[\"/uploads/img1.jpg\",\"/uploads/img2.jpg\"]".equals(saved.getImages())
        ));
        verify(orderMapper, times(1)).updateById(any(Order.class));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 订单状态不是已送达")
    void testSubmitFeedbacks_OrderNotCompleted() {
        Order pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setUserId(1L);
        pendingOrder.setStatus(OrderStatus.DELIVERING);

        FeedbackSubmitDTO feedback = buildFeedbackDTO(1L, 1L, 5, "测试评价", List.of());
        when(orderMapper.selectById(1L)).thenReturn(pendingOrder);

        var result = feedbackController.submitFeedbacks(List.of(feedback));

        assertFalse(result.isSuccess());
        assertEquals("Only completed orders can be reviewed", result.getMessage());
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 重复评价")
    void testSubmitFeedbacks_DuplicateReview() {
        FeedbackSubmitDTO feedback = buildFeedbackDTO(1L, 1L, 5, "测试评价", List.of());
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        var result = feedbackController.submitFeedbacks(List.of(feedback));

        assertFalse(result.isSuccess());
        assertEquals("该订单已评价，不能重复评价", result.getMessage());
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 验证订单状态更新为已评价")
    void testSubmitFeedbacks_OrderStatusUpdated() {
        FeedbackSubmitDTO feedback = buildFeedbackDTO(1L, 1L, 5, "非常好喝！", List.of());
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);

        feedbackController.submitFeedbacks(List.of(feedback));

        verify(orderMapper).updateById(argThat(order -> order.getStatus() == OrderStatus.REVIEWED));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 验证userId设置")
    void testSubmitFeedbacks_UserIdSet() {
        FeedbackSubmitDTO feedback = buildFeedbackDTO(1L, 1L, 5, "非常好喝！", List.of());
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        feedbackController.submitFeedbacks(List.of(feedback));

        verify(feedbackMapper).insert(argThat(saved -> saved.getUserId() != null && saved.getUserId().equals(1L)));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 评分超出范围")
    void testSubmitFeedbacks_RatingOutOfRange() {
        FeedbackSubmitDTO feedback = buildFeedbackDTO(1L, 1L, 0, "测试评价", List.of());
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        var result = feedbackController.submitFeedbacks(List.of(feedback));

        assertFalse(result.isSuccess());
        assertEquals("评分必须在1-5之间", result.getMessage());
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 商品ID为空")
    void testSubmitFeedbacks_ProductIdNull() {
        FeedbackSubmitDTO feedback = buildFeedbackDTO(1L, null, 5, "测试评价", List.of());
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        var result = feedbackController.submitFeedbacks(List.of(feedback));

        assertFalse(result.isSuccess());
        assertEquals("商品ID不能为空", result.getMessage());
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 订单ID为空")
    void testSubmitFeedbacks_OrderIdNull() {
        FeedbackSubmitDTO feedback = buildFeedbackDTO(null, 1L, 5, "测试评价", List.of());

        var result = feedbackController.submitFeedbacks(List.of(feedback));

        assertFalse(result.isSuccess());
        assertEquals("订单ID不能为空", result.getMessage());
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 订单ID不一致")
    void testSubmitFeedbacks_OrderIdMismatch() {
        FeedbackSubmitDTO feedback1 = buildFeedbackDTO(1L, 1L, 5, "测试评价1", List.of());
        FeedbackSubmitDTO feedback2 = buildFeedbackDTO(2L, 2L, 4, "测试评价2", List.of());
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        var result = feedbackController.submitFeedbacks(List.of(feedback1, feedback2));

        assertFalse(result.isSuccess());
        assertEquals("评价列表中的订单ID不一致", result.getMessage());
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 图片超过上限")
    void testSubmitFeedbacks_TooManyImages() {
        FeedbackSubmitDTO feedback = buildFeedbackDTO(
                1L,
                1L,
                5,
                "测试评价",
                List.of("/uploads/1.jpg", "/uploads/2.jpg", "/uploads/3.jpg", "/uploads/4.jpg")
        );
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        var result = feedbackController.submitFeedbacks(List.of(feedback));

        assertFalse(result.isSuccess());
        assertEquals("评价图片最多上传3张", result.getMessage());
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 使用旧Token格式（通过用户名查询）")
    void testSubmitFeedbacks_WithOldTokenFormat() {
        when(authentication.getDetails()).thenReturn("notALongType");
        when(authentication.getName()).thenReturn("testuser");
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        when(userService.getByUsername("testuser")).thenReturn(testUser);

        FeedbackSubmitDTO feedback = buildFeedbackDTO(1L, 1L, 5, "测试评价", List.of("/uploads/img1.jpg"));
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(feedbackMapper.insert(any(Feedback.class))).thenReturn(1);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        var result = feedbackController.submitFeedbacks(List.of(feedback));

        assertTrue(result.isSuccess());
        verify(userService, times(1)).getByUsername("testuser");
    }

    @Test
    @DisplayName("测试 submitFeedbacks - 完整流程验证（多商品评价）")
    void testSubmitFeedbacks_FullFlow() {
        FeedbackSubmitDTO feedback1 = buildFeedbackDTO(
                1L,
                1L,
                5,
                "珍珠奶茶非常好喝！",
                List.of("/uploads/img1.jpg", "/uploads/img2.jpg")
        );
        FeedbackSubmitDTO feedback2 = buildFeedbackDTO(1L, 2L, 4, "波波烤奶也不错", null);

        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(feedbackMapper.insert(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback insertedFeedback = invocation.getArgument(0);
            insertedFeedback.setId(3L);
            return 1;
        });
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        var result = feedbackController.submitFeedbacks(List.of(feedback1, feedback2));

        assertTrue(result.isSuccess());
        verify(feedbackMapper, times(2)).insert(any(Feedback.class));
        verify(feedbackMapper).insert(argThat(saved ->
                saved.getProductId().equals(1L)
                        && "[\"/uploads/img1.jpg\",\"/uploads/img2.jpg\"]".equals(saved.getImages())
        ));
        verify(feedbackMapper).insert(argThat(saved ->
                saved.getProductId().equals(2L)
                        && "[]".equals(saved.getImages())
        ));
        verify(orderMapper).updateById(argThat(order ->
                order.getId().equals(1L) && order.getStatus() == OrderStatus.REVIEWED
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
    @DisplayName("测试 getProductFeedbacks - 解析JSON图片数组")
    void testGetProductFeedbacks_ParseJsonImages() {
        testFeedback.setImages("[\"/uploads/img1.jpg\",\"/uploads/img2.jpg\"]");
        User user1 = new User();
        user1.setId(1L);
        user1.setNickname("用户1");
        user1.setAvatarUrl("/avatar/u1.png");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testFeedback));
        when(userService.listByIds(anyCollection())).thenReturn(List.of(user1));

        var result = feedbackController.getProductFeedbacks(1L, null, null, "desc");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals(List.of("/uploads/img1.jpg", "/uploads/img2.jpg"), result.getData().get(0).getImages());
        assertEquals("用户1", result.getData().get(0).getNickname());
        assertEquals("/avatar/u1.png", result.getData().get(0).getAvatarUrl());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 兼容旧逗号分隔图片")
    void testGetProductFeedbacks_ParseLegacyCsvImages() {
        testFeedback.setImages("/uploads/img1.jpg,/uploads/img2.jpg");
        User user1 = new User();
        user1.setId(1L);
        user1.setNickname("用户1");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testFeedback));
        when(userService.listByIds(anyCollection())).thenReturn(List.of(user1));

        var result = feedbackController.getProductFeedbacks(1L, null, null, "desc");

        assertTrue(result.isSuccess());
        assertEquals(List.of("/uploads/img1.jpg", "/uploads/img2.jpg"), result.getData().get(0).getImages());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 空JSON数组返回空列表")
    void testGetProductFeedbacks_EmptyJsonImages() {
        testFeedback.setImages("[]");
        User user1 = new User();
        user1.setId(1L);
        user1.setNickname("用户1");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testFeedback));
        when(userService.listByIds(anyCollection())).thenReturn(List.of(user1));

        var result = feedbackController.getProductFeedbacks(1L, null, null, "desc");

        assertTrue(result.isSuccess());
        assertTrue(result.getData().get(0).getImages().isEmpty());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 获取商品评价列表")
    void testGetProductFeedbacks() {
        User user1 = new User();
        user1.setId(1L);
        user1.setNickname("用户1");
        User user2 = new User();
        user2.setId(2L);
        user2.setNickname("用户2");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(testFeedback, testFeedback2));
        when(userService.listByIds(anyCollection())).thenReturn(Arrays.asList(user1, user2));

        var result = feedbackController.getProductFeedbacks(1L, null, null, "desc");

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals("用户1", result.getData().get(0).getNickname());
        assertEquals("用户2", result.getData().get(1).getNickname());
        verify(userService, times(1)).listByIds(anyCollection());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 无评价")
    void testGetProductFeedbacks_Empty() {
        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        var result = feedbackController.getProductFeedbacks(999L, null, null, "desc");

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
        verify(userService, never()).listByIds(anyCollection());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 按评分筛选")
    void testGetProductFeedbacks_FilterByRating() {
        User user1 = new User();
        user1.setId(1L);
        user1.setNickname("用户1");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testFeedback));
        when(userService.listByIds(anyCollection())).thenReturn(List.of(user1));

        var result = feedbackController.getProductFeedbacks(1L, 5, null, "desc");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals(5, result.getData().get(0).getRating());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 批量查询用户信息避免N+1")
    void testGetProductFeedbacks_BatchUserQuery() {
        User user1 = new User();
        user1.setId(1L);
        user1.setNickname("用户A");
        user1.setAvatarUrl("/avatar/a.png");
        User user2 = new User();
        user2.setId(2L);
        user2.setNickname("用户B");
        user2.setAvatarUrl("/avatar/b.png");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(testFeedback, testFeedback2));
        when(userService.listByIds(anyCollection())).thenReturn(Arrays.asList(user1, user2));

        feedbackController.getProductFeedbacks(1L, null, null, "desc");

        verify(userService, times(1)).listByIds(anyCollection());
        verify(userService, never()).getById(anyLong());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 用户不存在时显示匿名")
    void testGetProductFeedbacks_UserNotFound() {
        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testFeedback));
        when(userService.listByIds(anyCollection())).thenReturn(new ArrayList<>());

        var result = feedbackController.getProductFeedbacks(1L, null, null, "desc");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("匿名用户", result.getData().get(0).getNickname());
    }

    @Test
    @DisplayName("测试 getProductFeedbacks - 按有图筛选")
    void testGetProductFeedbacks_FilterByHasImage() {
        testFeedback.setImages("[\"/uploads/img1.jpg\",\"/uploads/img2.jpg\"]");
        User user1 = new User();
        user1.setId(1L);
        user1.setNickname("用户1");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testFeedback));
        when(userService.listByIds(anyCollection())).thenReturn(List.of(user1));

        var result = feedbackController.getProductFeedbacks(1L, null, true, "desc");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertFalse(result.getData().get(0).getImages().isEmpty());
    }

    @Test
    @DisplayName("测试 convertToVOList - 包含adminReply")
    void testConvertToVOList_WithAdminReply() {
        testFeedback.setAdminReply("感谢您的评价！");
        User user1 = new User();
        user1.setId(1L);
        user1.setNickname("用户1");

        when(feedbackMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(testFeedback));
        when(userService.listByIds(anyCollection())).thenReturn(List.of(user1));

        var result = feedbackController.getProductFeedbacks(1L, null, null, "desc");

        assertTrue(result.isSuccess());
        assertEquals("感谢您的评价！", result.getData().get(0).getAdminReply());
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

    private FeedbackSubmitDTO buildFeedbackDTO(Long orderId, Long productId, Integer rating, String content, List<String> images) {
        FeedbackSubmitDTO dto = new FeedbackSubmitDTO();
        dto.setOrderId(orderId);
        dto.setProductId(productId);
        dto.setRating(rating);
        dto.setContent(content);
        dto.setImages(images);
        return dto;
    }
}

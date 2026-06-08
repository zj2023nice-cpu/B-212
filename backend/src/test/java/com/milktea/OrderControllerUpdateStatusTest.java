package com.milktea;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.milktea.common.ErrorCode;
import com.milktea.controller.OrderController;
import com.milktea.entity.*;
import com.milktea.enums.OrderStatus;
import com.milktea.exception.BusinessException;
import com.milktea.mapper.*;
import com.milktea.service.ProductService;
import com.milktea.service.CouponService;
import com.milktea.service.MemberService;
import com.milktea.service.AddressService;
import com.milktea.service.NotificationService;
import com.milktea.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController.updateStatus 全面的单元测试")
class OrderControllerUpdateStatusTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductService productService;

    @Mock
    private CouponService couponService;

    @Mock
    private MemberService memberService;

    @Mock
    private AddressService addressService;

    @Mock
    private OrderCancelLogMapper orderCancelLogMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private OrderController orderController;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private OrderItem defaultOrderItem;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        defaultOrderItem = new OrderItem();
        defaultOrderItem.setId(1L);
        defaultOrderItem.setOrderId(1L);
        defaultOrderItem.setProductId(100L);
        defaultOrderItem.setProductName("珍珠奶茶");
        defaultOrderItem.setProductPrice(new BigDecimal("15.00"));
        defaultOrderItem.setQuantity(2);
    }

    private void setupUserAuthentication() {
        when(authentication.getDetails()).thenReturn(1L);
    }

    private void setupAdminAuthentication() {
        when(authentication.getDetails()).thenReturn(2L);
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
    }

    private Order createOrder(Long id, OrderStatus status) {
        return createOrder(id, 1L, status, new BigDecimal("30.00"));
    }

    private Order createOrder(Long id, Long userId, OrderStatus status, BigDecimal payAmount) {
        Order order = new Order();
        order.setId(id);
        order.setOrderSn("ORD" + id);
        order.setUserId(userId);
        order.setStatus(status);
        order.setPayAmount(payAmount);
        order.setTotalAmount(payAmount);
        return order;
    }

    private void mockCancelSuccess(Order order) {
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(defaultOrderItem));
        when(productService.restoreStock(anyLong(), anyInt())).thenReturn(true);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        when(orderCancelLogMapper.insert(any(OrderCancelLog.class))).thenReturn(1);
    }

    @Nested
    @DisplayName("合法状态流转测试")
    class LegalTransitionTests {

        @Test
        @DisplayName("PENDING_PAYMENT -> PAID: 用户支付订单")
        void pendingPaymentToPaid_ByUser() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            var result = orderController.updateStatus(1L, "PAID");

            assertTrue(result.isSuccess());
            verify(memberService, times(1)).earnPoints(eq(1L), eq(1L), eq(new BigDecimal("30.00")));
            verify(notificationService, times(1)).sendNotification(eq(1L), eq("订单已支付"), anyString(), eq("ORDER"), eq(1L));
        }

        @Test
        @DisplayName("PENDING_PAYMENT -> PAID: 积分发放失败不影响订单支付")
        void pendingPaymentToPaid_PointsEarnFailureDoesNotAffect() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);
            doThrow(new RuntimeException("积分服务异常")).when(memberService).earnPoints(anyLong(), anyLong(), any(BigDecimal.class));

            var result = orderController.updateStatus(1L, "PAID");

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("PENDING_PAYMENT -> PAID: 并发状态变更检测")
        void pendingPaymentToPaid_ConcurrentStatusChange() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);

            Order refreshedOrder = createOrder(1L, OrderStatus.PAID);

            when(orderMapper.selectById(1L)).thenReturn(order, refreshedOrder);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "PAID"));

            assertEquals(ErrorCode.B0048, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("请刷新"));
        }

        @Test
        @DisplayName("PENDING_PAYMENT -> PAID: 通知发送失败不影响支付")
        void pendingPaymentToPaid_NotificationFailureDoesNotAffect() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);
            doThrow(new RuntimeException("通知服务异常")).when(notificationService).sendNotification(anyLong(), anyString(), anyString(), anyString(), anyLong());

            var result = orderController.updateStatus(1L, "PAID");

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("PENDING_PAYMENT -> CANCELLED: 用户取消待支付订单，无积分扣减")
        void pendingPaymentToCancelled_ByUser() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);

            when(orderMapper.selectById(1L)).thenReturn(order);
            mockCancelSuccess(order);

            var result = orderController.updateStatus(1L, "CANCELLED");

            assertTrue(result.isSuccess());
            verify(productService, times(1)).restoreStock(100L, 2);
            verify(memberService, never()).deductPoints(anyLong(), anyLong(), any(BigDecimal.class));
            verify(orderCancelLogMapper, times(1)).insert(argThat(log ->
                    log.getOperator().equals("USER") && log.getCancelReason().equals("用户主动取消")
            ));
        }

        @Test
        @DisplayName("PAID -> CANCELLED: 用户取消已支付订单，需扣减积分")
        void paidToCancelled_ByUser_RequiresPointsDeduction() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PAID);

            when(orderMapper.selectById(1L)).thenReturn(order);
            mockCancelSuccess(order);

            var result = orderController.updateStatus(1L, "CANCELLED");

            assertTrue(result.isSuccess());
            verify(productService, times(1)).restoreStock(100L, 2);
            verify(memberService, times(1)).deductPoints(eq(1L), eq(1L), eq(new BigDecimal("30.00")));
            verify(orderCancelLogMapper, times(1)).insert(argThat(log ->
                    log.getOperator().equals("USER")
            ));
        }

        @Test
        @DisplayName("PAID -> CANCELLED: 积分扣减失败不影响取消")
        void paidToCancelled_PointsDeductFailureDoesNotAffect() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PAID);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(defaultOrderItem));
            when(productService.restoreStock(anyLong(), anyInt())).thenReturn(true);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);
            when(orderCancelLogMapper.insert(any(OrderCancelLog.class))).thenReturn(1);
            doThrow(new RuntimeException("积分扣减失败")).when(memberService).deductPoints(anyLong(), anyLong(), any(BigDecimal.class));

            var result = orderController.updateStatus(1L, "CANCELLED");

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("PREPARING -> CANCELLED: 用户取消制作中的订单")
        void preparingToCancelled_ByUser() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);

            when(orderMapper.selectById(1L)).thenReturn(order);
            mockCancelSuccess(order);

            var result = orderController.updateStatus(1L, "CANCELLED");

            assertTrue(result.isSuccess());
            verify(productService, times(1)).restoreStock(100L, 2);
            verify(memberService, times(1)).deductPoints(eq(1L), eq(1L), eq(new BigDecimal("30.00")));
        }

        @Test
        @DisplayName("DELIVERING -> CANCELLED: 用户取消配送中的订单")
        void deliveringToCancelled_ByUser() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.DELIVERING);

            when(orderMapper.selectById(1L)).thenReturn(order);
            mockCancelSuccess(order);

            var result = orderController.updateStatus(1L, "CANCELLED");

            assertTrue(result.isSuccess());
            verify(productService, times(1)).restoreStock(100L, 2);
            verify(memberService, times(1)).deductPoints(eq(1L), eq(1L), eq(new BigDecimal("30.00")));
        }

        @Test
        @DisplayName("DELIVERING -> COMPLETED: 用户确认收货")
        void deliveringToCompleted_ByUser() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.DELIVERING);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            var result = orderController.updateStatus(1L, "COMPLETED");

            assertTrue(result.isSuccess());
            verify(orderMapper, times(1)).updateById(argThat(o ->
                    o.getStatus() == OrderStatus.COMPLETED
            ));
        }

        @Test
        @DisplayName("完整状态流转链: PENDING_PAYMENT -> PAID -> PREPARING -> DELIVERING -> COMPLETED -> REVIEWED (管理员)")
        void fullStatusTransitionChain_ByAdmin() {
            setupAdminAuthentication();
            Long orderId = 1L;

            Order order = createOrder(orderId, OrderStatus.PENDING_PAYMENT);
            when(orderMapper.selectById(orderId)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            var result = orderController.updateStatus(orderId, "PAID");
            assertTrue(result.isSuccess());
            verify(memberService).earnPoints(anyLong(), anyLong(), any(BigDecimal.class));

            order.setStatus(OrderStatus.PAID);
            var result2 = orderController.updateStatus(orderId, "PREPARING");
            assertTrue(result2.isSuccess());

            order.setStatus(OrderStatus.PREPARING);
            var result3 = orderController.updateStatus(orderId, "DELIVERING");
            assertTrue(result3.isSuccess());

            order.setStatus(OrderStatus.DELIVERING);
            var result4 = orderController.updateStatus(orderId, "COMPLETED");
            assertTrue(result4.isSuccess());

            order.setStatus(OrderStatus.COMPLETED);
            var result5 = orderController.updateStatus(orderId, "REVIEWED");
            assertTrue(result5.isSuccess());
        }
    }

    @Nested
    @DisplayName("管理员专属状态流转测试")
    class AdminOnlyTransitionTests {

        @Test
        @DisplayName("PAID -> PREPARING: 管理员开始制作")
        void paidToPreparing_ByAdmin() {
            setupAdminAuthentication();
            Order order = createOrder(1L, OrderStatus.PAID);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            var result = orderController.updateStatus(1L, "PREPARING");

            assertTrue(result.isSuccess());
            verify(orderMapper, times(1)).updateById(argThat(o ->
                    o.getStatus() == OrderStatus.PREPARING
            ));
        }

        @Test
        @DisplayName("PREPARING -> DELIVERING: 管理员开始配送")
        void preparingToDelivering_ByAdmin() {
            setupAdminAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            var result = orderController.updateStatus(1L, "DELIVERING");

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("COMPLETED -> REVIEWED: 管理员标记已评价")
        void completedToReviewed_ByAdmin() {
            setupAdminAuthentication();
            Order order = createOrder(1L, OrderStatus.COMPLETED);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            var result = orderController.updateStatus(1L, "REVIEWED");

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("管理员取消订单记录 ADMIN 操作者")
        void adminCancelOrder_RecordsAdminOperator() {
            setupAdminAuthentication();
            Order order = createOrder(1L, OrderStatus.PAID);

            when(orderMapper.selectById(1L)).thenReturn(order);
            mockCancelSuccess(order);

            var result = orderController.updateStatus(1L, "CANCELLED");

            assertTrue(result.isSuccess());
            verify(orderCancelLogMapper, times(1)).insert(argThat(log ->
                    log.getOperator().equals("ADMIN") && log.getCancelReason().equals("管理员取消")
            ));
        }
    }

    @Nested
    @DisplayName("非法状态流转测试")
    class IllegalTransitionTests {

        static Stream<Arguments> illegalTransitionsForUser() {
            return Stream.of(
                    Arguments.of(OrderStatus.PAID, "PREPARING", "用户不能执行 PAID->PREPARING (管理员专属)"),
                    Arguments.of(OrderStatus.PREPARING, "DELIVERING", "用户不能执行 PREPARING->DELIVERING (管理员专属)"),
                    Arguments.of(OrderStatus.COMPLETED, "REVIEWED", "用户不能执行 COMPLETED->REVIEWED (管理员专属)"),
                    Arguments.of(OrderStatus.PENDING_PAYMENT, "PREPARING", "PENDING_PAYMENT 不能直接到 PREPARING"),
                    Arguments.of(OrderStatus.PENDING_PAYMENT, "DELIVERING", "PENDING_PAYMENT 不能直接到 DELIVERING"),
                    Arguments.of(OrderStatus.PENDING_PAYMENT, "COMPLETED", "PENDING_PAYMENT 不能直接到 COMPLETED"),
                    Arguments.of(OrderStatus.PAID, "DELIVERING", "PAID 不能直接到 DELIVERING"),
                    Arguments.of(OrderStatus.PAID, "COMPLETED", "PAID 不能直接到 COMPLETED"),
                    Arguments.of(OrderStatus.PREPARING, "COMPLETED", "PREPARING 不能直接到 COMPLETED"),
                    Arguments.of(OrderStatus.PREPARING, "PAID", "PREPARING 不能回退到 PAID"),
                    Arguments.of(OrderStatus.DELIVERING, "PAID", "DELIVERING 不能回退到 PAID"),
                    Arguments.of(OrderStatus.DELIVERING, "PREPARING", "DELIVERING 不能回退到 PREPARING"),
                    Arguments.of(OrderStatus.COMPLETED, "CANCELLED", "COMPLETED 不能取消"),
                    Arguments.of(OrderStatus.COMPLETED, "PAID", "COMPLETED 不能回退到 PAID")
            );
        }

        @ParameterizedTest(name = "{2}")
        @MethodSource("illegalTransitionsForUser")
        @DisplayName("用户非法状态流转应抛出 BusinessException")
        void illegalTransition_ByUser_ThrowsBusinessException(OrderStatus from, String target, String description) {
            setupUserAuthentication();
            Order order = createOrder(1L, from);

            when(orderMapper.selectById(1L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, target));

            assertEquals(ErrorCode.B0008, ex.getErrorCode());
            assertEquals("Invalid status transition", ex.getMessage());
        }

        static Stream<Arguments> illegalTransitionsForAdmin() {
            return Stream.of(
                    Arguments.of(OrderStatus.PENDING_PAYMENT, "DELIVERING", "PENDING_PAYMENT 不能直接到 DELIVERING"),
                    Arguments.of(OrderStatus.PENDING_PAYMENT, "COMPLETED", "PENDING_PAYMENT 不能直接到 COMPLETED"),
                    Arguments.of(OrderStatus.PAID, "DELIVERING", "PAID 不能直接到 DELIVERING"),
                    Arguments.of(OrderStatus.PAID, "COMPLETED", "PAID 不能直接到 COMPLETED"),
                    Arguments.of(OrderStatus.PREPARING, "COMPLETED", "PREPARING 不能直接到 COMPLETED"),
                    Arguments.of(OrderStatus.PREPARING, "PAID", "PREPARING 不能回退到 PAID"),
                    Arguments.of(OrderStatus.COMPLETED, "CANCELLED", "COMPLETED 不能取消"),
                    Arguments.of(OrderStatus.COMPLETED, "PAID", "COMPLETED 不能回退到 PAID")
            );
        }

        @ParameterizedTest(name = "{2}")
        @MethodSource("illegalTransitionsForAdmin")
        @DisplayName("管理员非法状态流转应抛出 BusinessException")
        void illegalTransition_ByAdmin_ThrowsBusinessException(OrderStatus from, String target, String description) {
            setupAdminAuthentication();
            Order order = createOrder(1L, from);

            when(orderMapper.selectById(1L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, target));

            assertEquals(ErrorCode.B0008, ex.getErrorCode());
            assertEquals("Invalid status transition", ex.getMessage());
        }

        @Test
        @DisplayName("无效的状态字符串应抛出 BusinessException")
        void invalidStatusString_ThrowsBusinessException() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);
            when(orderMapper.selectById(1L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "INVALID_STATUS"));

            assertEquals(ErrorCode.B0008, ex.getErrorCode());
            assertEquals("Invalid order status", ex.getMessage());
        }

        @Test
        @DisplayName("不存在的枚举值应抛出 BusinessException")
        void nonexistentStatusEnum_ThrowsBusinessException() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "NONEXISTENT"));
        }

        @Test
        @DisplayName("CANCELLED 是终态，不能再更新")
        void cancelledIsTerminal_CannotUpdate() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.CANCELLED);
            when(orderMapper.selectById(1L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "PENDING_PAYMENT"));

            assertEquals(ErrorCode.B0008, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("已取消"));
        }

        @Test
        @DisplayName("REVIEWED 是终态，不能再更新")
        void reviewedIsTerminal_CannotUpdate() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.REVIEWED);
            when(orderMapper.selectById(1L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "CANCELLED"));

            assertEquals(ErrorCode.B0008, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("已评价"));
        }

        @ParameterizedTest
        @MethodSource("terminalStateTransitions")
        @DisplayName("终态订单任何状态变更都应失败")
        void terminalState_CannotTransitionToAny(OrderStatus terminal, String target) {
            setupAdminAuthentication();
            Order order = createOrder(1L, terminal);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, target));
        }

        static Stream<Arguments> terminalStateTransitions() {
            return Stream.of(
                    Arguments.of(OrderStatus.CANCELLED, "PENDING_PAYMENT"),
                    Arguments.of(OrderStatus.CANCELLED, "PAID"),
                    Arguments.of(OrderStatus.CANCELLED, "PREPARING"),
                    Arguments.of(OrderStatus.CANCELLED, "DELIVERING"),
                    Arguments.of(OrderStatus.CANCELLED, "COMPLETED"),
                    Arguments.of(OrderStatus.REVIEWED, "PENDING_PAYMENT"),
                    Arguments.of(OrderStatus.REVIEWED, "PAID"),
                    Arguments.of(OrderStatus.REVIEWED, "PREPARING"),
                    Arguments.of(OrderStatus.REVIEWED, "CANCELLED"),
                    Arguments.of(OrderStatus.REVIEWED, "COMPLETED")
            );
        }
    }

    @Nested
    @DisplayName("用户与管理员权限差异测试")
    class PermissionDifferenceTests {

        @Test
        @DisplayName("普通用户不能执行 PAID -> PREPARING (管理员专属)")
        void userCannotDo_PaidToPreparing() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PAID);
            when(orderMapper.selectById(1L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "PREPARING"));

            assertEquals(ErrorCode.B0008, ex.getErrorCode());
        }

        @Test
        @DisplayName("普通用户不能执行 PREPARING -> DELIVERING (管理员专属)")
        void userCannotDo_PreparingToDelivering() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);
            when(orderMapper.selectById(1L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "DELIVERING"));

            assertEquals(ErrorCode.B0008, ex.getErrorCode());
        }

        @Test
        @DisplayName("普通用户不能执行 COMPLETED -> REVIEWED (管理员专属)")
        void userCannotDo_CompletedToReviewed() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.COMPLETED);
            when(orderMapper.selectById(1L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "REVIEWED"));

            assertEquals(ErrorCode.B0008, ex.getErrorCode());
        }

        @Test
        @DisplayName("管理员可以执行 PAID -> PREPARING")
        void adminCanDo_PaidToPreparing() {
            setupAdminAuthentication();
            Order order = createOrder(1L, OrderStatus.PAID);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            var result = orderController.updateStatus(1L, "PREPARING");

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("管理员可以执行 PREPARING -> DELIVERING")
        void adminCanDo_PreparingToDelivering() {
            setupAdminAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            var result = orderController.updateStatus(1L, "DELIVERING");

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("管理员可以执行 COMPLETED -> REVIEWED")
        void adminCanDo_CompletedToReviewed() {
            setupAdminAuthentication();
            Order order = createOrder(1L, OrderStatus.COMPLETED);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            var result = orderController.updateStatus(1L, "REVIEWED");

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("普通用户可以取消自己各阶段的订单")
        void userCanCancelOwnOrder_FromVariousStates() {
            setupUserAuthentication();

            List<OrderStatus> cancellableStates = Arrays.asList(
                    OrderStatus.PENDING_PAYMENT,
                    OrderStatus.PAID,
                    OrderStatus.PREPARING,
                    OrderStatus.DELIVERING
            );

            for (OrderStatus status : cancellableStates) {
                Order order = createOrder(1L, status);
                when(orderMapper.selectById(1L)).thenReturn(order);
                mockCancelSuccess(order);

                var result = orderController.updateStatus(1L, "CANCELLED");

                assertTrue(result.isSuccess(), "用户应能从 " + status + " 取消订单");
                reset(orderMapper, orderItemMapper, productService, orderCancelLogMapper);
            }
        }

        @Test
        @DisplayName("管理员也可以取消各阶段的订单")
        void adminCanCancelOrder_FromVariousStates() {
            setupAdminAuthentication();

            List<OrderStatus> cancellableStates = Arrays.asList(
                    OrderStatus.PENDING_PAYMENT,
                    OrderStatus.PAID,
                    OrderStatus.PREPARING,
                    OrderStatus.DELIVERING
            );

            for (OrderStatus status : cancellableStates) {
                Order order = createOrder(1L, status);
                when(orderMapper.selectById(1L)).thenReturn(order);
                mockCancelSuccess(order);

                var result = orderController.updateStatus(1L, "CANCELLED");

                assertTrue(result.isSuccess(), "管理员应能从 " + status + " 取消订单");
                reset(orderMapper, orderItemMapper, productService, orderCancelLogMapper);
            }
        }
    }

    @Nested
    @DisplayName("取消订单库存恢复测试")
    class StockRecoveryTests {

        @Test
        @DisplayName("取消订单时所有商品库存恢复成功")
        void cancelOrder_AllStockRestoredSuccessfully() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);

            OrderItem item1 = new OrderItem();
            item1.setProductId(100L);
            item1.setProductName("珍珠奶茶");
            item1.setQuantity(2);

            OrderItem item2 = new OrderItem();
            item2.setProductId(200L);
            item2.setProductName("芋泥波波");
            item2.setQuantity(1);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(item1, item2));
            when(productService.restoreStock(100L, 2)).thenReturn(true);
            when(productService.restoreStock(200L, 1)).thenReturn(true);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);
            when(orderCancelLogMapper.insert(any(OrderCancelLog.class))).thenReturn(1);

            var result = orderController.updateStatus(1L, "CANCELLED");

            assertTrue(result.isSuccess());
            verify(productService, times(1)).restoreStock(100L, 2);
            verify(productService, times(1)).restoreStock(200L, 1);
            verify(orderMapper, times(1)).updateById(any(Order.class));
            verify(orderCancelLogMapper, times(1)).insert(any(OrderCancelLog.class));
        }

        @Test
        @DisplayName("取消订单时部分商品库存恢复失败应抛出异常")
        void cancelOrder_SomeStockRestoreFails_ThrowsException() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);

            OrderItem item1 = new OrderItem();
            item1.setProductId(100L);
            item1.setProductName("珍珠奶茶");
            item1.setQuantity(2);

            OrderItem item2 = new OrderItem();
            item2.setProductId(200L);
            item2.setProductName("芋泥波波");
            item2.setQuantity(1);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(item1, item2));
            when(productService.restoreStock(100L, 2)).thenReturn(true);
            when(productService.restoreStock(200L, 1)).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "CANCELLED"));

            assertEquals(ErrorCode.B0011, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("库存恢复失败"));
            assertTrue(ex.getMessage().contains("1个商品"));
        }

        @Test
        @DisplayName("取消订单时全部商品库存恢复失败应抛出异常")
        void cancelOrder_AllStockRestoreFails_ThrowsException() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(defaultOrderItem));
            when(productService.restoreStock(anyLong(), anyInt())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "CANCELLED"));

            assertEquals(ErrorCode.B0011, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("1个商品库存恢复失败"));
        }

        @Test
        @DisplayName("库存恢复失败时不应更新订单状态")
        void cancelOrder_StockRestoreFails_OrderNotUpdated() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(defaultOrderItem));
            when(productService.restoreStock(anyLong(), anyInt())).thenReturn(false);

            assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "CANCELLED"));

            verify(orderMapper, never()).updateById(any(Order.class));
            verify(orderCancelLogMapper, never()).insert(any(OrderCancelLog.class));
        }

        @Test
        @DisplayName("库存恢复失败时不应记录取消日志")
        void cancelOrder_StockRestoreFails_CancelLogNotInserted() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(defaultOrderItem));
            when(productService.restoreStock(anyLong(), anyInt())).thenReturn(false);

            assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "CANCELLED"));

            verify(orderCancelLogMapper, never()).insert(any(OrderCancelLog.class));
        }

        @Test
        @DisplayName("取消订单时取消通知失败不影响取消流程")
        void cancelOrder_NotificationFailureDoesNotAffect() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);

            when(orderMapper.selectById(1L)).thenReturn(order);
            mockCancelSuccess(order);
            doThrow(new RuntimeException("通知服务异常"))
                    .when(notificationService).sendNotification(anyLong(), eq("订单已取消"), anyString(), anyString(), anyLong());

            var result = orderController.updateStatus(1L, "CANCELLED");

            assertTrue(result.isSuccess());
            verify(orderMapper, times(1)).updateById(any(Order.class));
            verify(orderCancelLogMapper, times(1)).insert(any(OrderCancelLog.class));
        }

        @Test
        @DisplayName("待支付订单取消不扣减积分，已支付订单取消需扣减积分")
        void cancelOrder_PointsDeductionDiffersByCurrentStatus() {
            setupUserAuthentication();

            Order pendingPaymentOrder = createOrder(1L, OrderStatus.PENDING_PAYMENT);
            when(orderMapper.selectById(1L)).thenReturn(pendingPaymentOrder);
            mockCancelSuccess(pendingPaymentOrder);

            orderController.updateStatus(1L, "CANCELLED");
            verify(memberService, never()).deductPoints(anyLong(), anyLong(), any(BigDecimal.class));

            reset(orderMapper, orderItemMapper, productService, orderCancelLogMapper, memberService);

            Order paidOrder = createOrder(2L, OrderStatus.PAID);
            when(orderMapper.selectById(2L)).thenReturn(paidOrder);
            mockCancelSuccess(paidOrder);

            orderController.updateStatus(2L, "CANCELLED");
            verify(memberService, times(1)).deductPoints(eq(1L), eq(2L), eq(new BigDecimal("30.00")));
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("订单不存在时 selectById 返回 null 导致 NPE")
        void orderNotFound_ThrowsNPE() {
            setupUserAuthentication();
            when(orderMapper.selectById(999L)).thenReturn(null);

            assertThrows(NullPointerException.class,
                    () -> orderController.updateStatus(999L, "PAID"));
        }

        @Test
        @DisplayName("非法状态字符串抛出 BusinessException(B0008)")
        void invalidStatus_ThrowsBusinessExceptionWithB0008() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);
            when(orderMapper.selectById(1L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "GIBBERISH"));

            assertEquals(ErrorCode.B0008, ex.getErrorCode());
            assertEquals("Invalid order status", ex.getMessage());
        }

        @Test
        @DisplayName("空字符串状态值抛出 BusinessException")
        void emptyStatus_ThrowsBusinessException() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, ""));
        }

        @Test
        @DisplayName("非法状态流转的错误码应为 B0008")
        void illegalTransition_ErrorCodeIsB0008() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.COMPLETED);
            when(orderMapper.selectById(1L)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "CANCELLED"));

            assertEquals(ErrorCode.B0008, ex.getErrorCode());
        }

        @Test
        @DisplayName("库存恢复失败的错误码应为 B0011")
        void stockRestoreFailure_ErrorCodeIsB0011() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(defaultOrderItem));
            when(productService.restoreStock(anyLong(), anyInt())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "CANCELLED"));

            assertEquals(ErrorCode.B0011, ex.getErrorCode());
        }

        @Test
        @DisplayName("并发状态变更的错误码应为 B0048")
        void concurrentStatusChange_ErrorCodeIsB0048() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);
            Order refreshed = createOrder(1L, OrderStatus.PAID);

            when(orderMapper.selectById(1L)).thenReturn(order, refreshed);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderController.updateStatus(1L, "PAID"));

            assertEquals(ErrorCode.B0048, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("状态变更通知测试")
    class NotificationTests {

        @Test
        @DisplayName("支付成功后发送支付通知")
        void paidTransition_SendsPaymentNotification() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            orderController.updateStatus(1L, "PAID");

            verify(notificationService, times(1)).sendNotification(eq(1L), eq("订单已支付"), contains("ORD1"), eq("ORDER"), eq(1L));
        }

        @Test
        @DisplayName("普通状态变更发送状态变更通知")
        void normalTransition_SendsStatusChangeNotification() {
            setupAdminAuthentication();
            Order order = createOrder(1L, OrderStatus.PAID);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            orderController.updateStatus(1L, "PREPARING");

            verify(notificationService, times(1)).sendNotification(eq(1L), eq("订单状态变更"), contains("制作中"), eq("ORDER"), eq(1L));
        }

        @Test
        @DisplayName("取消订单发送取消通知")
        void cancelTransition_SendsCancelNotification() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);
            when(orderMapper.selectById(1L)).thenReturn(order);
            mockCancelSuccess(order);

            orderController.updateStatus(1L, "CANCELLED");

            verify(notificationService, times(1)).sendNotification(eq(1L), eq("订单已取消"), anyString(), eq("ORDER"), eq(1L));
        }

        @Test
        @DisplayName("支付后不再发送重复的普通状态变更通知")
        void paidTransition_DoesNotSendDuplicateNotification() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PENDING_PAYMENT);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            orderController.updateStatus(1L, "PAID");

            verify(notificationService, times(1)).sendNotification(eq(1L), anyString(), anyString(), anyString(), anyLong());
        }
    }

    @Nested
    @DisplayName("订单更新验证测试")
    class OrderUpdateVerificationTests {

        @Test
        @DisplayName("非取消状态变更只更新 status 字段")
        void nonCancelTransition_OnlyUpdatesStatus() {
            setupAdminAuthentication();
            Order order = createOrder(1L, OrderStatus.PAID);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            orderController.updateStatus(1L, "PREPARING");

            verify(orderMapper, times(1)).updateById(argThat(o ->
                    o.getStatus() == OrderStatus.PREPARING && o.getCancelReason() == null
            ));
        }

        @Test
        @DisplayName("取消状态变更同时更新 status 和 cancelReason")
        void cancelTransition_UpdatesStatusAndCancelReason() {
            setupUserAuthentication();
            Order order = createOrder(1L, OrderStatus.PREPARING);
            when(orderMapper.selectById(1L)).thenReturn(order);
            mockCancelSuccess(order);

            orderController.updateStatus(1L, "CANCELLED");

            verify(orderMapper, times(1)).updateById(argThat(o ->
                    o.getStatus() == OrderStatus.CANCELLED && o.getCancelReason() != null
            ));
        }

        @Test
        @DisplayName("成功更新后返回 Result.success")
        void successfulTransition_ReturnsSuccessResult() {
            setupAdminAuthentication();
            Order order = createOrder(1L, OrderStatus.PAID);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);

            var result = orderController.updateStatus(1L, "PREPARING");

            assertTrue(result.isSuccess());
            assertEquals("Status updated", result.getData());
        }
    }
}

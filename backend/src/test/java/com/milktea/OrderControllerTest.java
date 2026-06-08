package com.milktea;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.controller.OrderController;
import com.milktea.entity.*;
import com.milktea.mapper.*;
import com.milktea.service.ProductService;
import com.milktea.service.UserService;
import com.milktea.service.CouponService;
import com.milktea.service.MemberService;
import com.milktea.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController 测试")
class OrderControllerTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private UserService userService;

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

    @InjectMocks
    private OrderController orderController;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User testUser;
    private Product testProduct;
    private CartItem testCartItem;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole("USER");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("珍珠奶茶");
        testProduct.setPrice(new BigDecimal("15.00"));
        testProduct.setStock(10);

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setUserId(1L);
        testCartItem.setProductId(1L);
        testCartItem.setQuantity(2);
        testCartItem.setSpecs("中杯, 少糖");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderSn("TEST123456");
        testOrder.setUserId(1L);
        testOrder.setTotalAmount(new BigDecimal("30.00"));
        testOrder.setStatus(1);

        testOrderItem = new OrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setOrderId(1L);
        testOrderItem.setProductId(1L);
        testOrderItem.setProductName("珍珠奶茶");
        testOrderItem.setProductPrice(new BigDecimal("15.00"));
        testOrderItem.setQuantity(2);
        testOrderItem.setSpecs("中杯, 少糖");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
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

    @Test
    @DisplayName("测试 createOrder - 购物车为空")
    void testCreateOrder_CartEmpty() {
        setupUserAuthentication();
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        
        var result = orderController.createOrder(new Order());
        
        assertFalse(result.isSuccess());
        assertEquals("Cart is empty", result.getMessage());
    }

    @Test
    @DisplayName("测试 createOrder - 成功创建订单")
    void testCreateOrder_Success() {
        setupUserAuthentication();
        List<CartItem> cartItems = Collections.singletonList(testCartItem);
        
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(cartItems);
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productService.deductStockWithRetry(eq(1L), eq(2), anyInt())).thenReturn(true);
        when(memberService.calculateDiscount(eq(1L), any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);
        when(cartItemMapper.deleteById(1L)).thenReturn(1);
        
        var result = orderController.createOrder(new Order());
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        verify(orderMapper, times(1)).insert(any(Order.class));
        verify(orderItemMapper, times(1)).insert(any(OrderItem.class));
        verify(cartItemMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("测试 getMyOrders - 获取订单列表")
    void testGetMyOrders() {
        setupUserAuthentication();
        Page<Order> page = new Page<>();
        page.setRecords(Collections.singletonList(testOrder));
        page.setTotal(1);
        
        when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        
        var result = orderController.getMyOrders(1, 10);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getRecords().size());
    }

    @Test
    @DisplayName("测试 getOrderDetail - 订单不存在")
    void testGetOrderDetail_OrderNotFound() {
        setupUserAuthentication();
        when(orderMapper.selectById(999L)).thenReturn(null);
        
        var result = orderController.getOrderDetail(999L);
        
        assertFalse(result.isSuccess());
        assertEquals("Order not found", result.getMessage());
    }

    @Test
    @DisplayName("测试 getOrderDetail - 无权访问")
    void testGetOrderDetail_NotAuthorized() {
        setupUserAuthentication();
        Order otherUserOrder = new Order();
        otherUserOrder.setId(2L);
        otherUserOrder.setUserId(999L);
        
        when(orderMapper.selectById(2L)).thenReturn(otherUserOrder);
        when(authentication.getAuthorities()).thenReturn(new ArrayList<>());
        
        var result = orderController.getOrderDetail(2L);
        
        assertFalse(result.isSuccess());
        assertEquals("Not authorized to view this order", result.getMessage());
    }

    @Test
    @DisplayName("测试 getOrderDetail - 成功获取订单详情")
    void testGetOrderDetail_Success() {
        setupUserAuthentication();
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(authentication.getAuthorities()).thenReturn(new ArrayList<>());
        
        var result = orderController.getOrderDetail(1L);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
    }

    @Test
    @DisplayName("测试 getOrderDetail - 管理员可以查看任何订单")
    void testGetOrderDetail_AdminCanViewAnyOrder() {
        setupAdminAuthentication();
        Order otherUserOrder = new Order();
        otherUserOrder.setId(2L);
        otherUserOrder.setUserId(999L);
        
        when(orderMapper.selectById(2L)).thenReturn(otherUserOrder);
        
        var result = orderController.getOrderDetail(2L);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("测试 getOrderItems - 获取订单项")
    void testGetOrderItems() {
        setupUserAuthentication();
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(testOrderItem));
        
        var result = orderController.getOrderItems(1L);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("测试 updateStatus - 订单不存在")
    void testUpdateStatus_OrderNotFound() {
        setupUserAuthentication();
        when(orderMapper.selectById(999L)).thenReturn(null);
        
        var result = orderController.updateStatus(999L, 2);
        
        assertFalse(result.isSuccess());
        assertEquals("Order not found", result.getMessage());
    }

    @Test
    @DisplayName("测试 updateStatus - 无效状态值")
    void testUpdateStatus_InvalidStatus() {
        setupUserAuthentication();
        when(orderMapper.selectById(1L)).thenReturn(testOrder);
        
        var result = orderController.updateStatus(1L, 10);
        
        assertFalse(result.isSuccess());
        assertEquals("Invalid order status", result.getMessage());
    }

    @Test
    @DisplayName("测试 updateStatus - 已取消订单不能更新")
    void testUpdateStatus_CannotUpdateCancelledOrder() {
        setupUserAuthentication();
        Order cancelledOrder = new Order();
        cancelledOrder.setId(1L);
        cancelledOrder.setUserId(1L);
        cancelledOrder.setStatus(3);
        
        when(orderMapper.selectById(1L)).thenReturn(cancelledOrder);
        
        var result = orderController.updateStatus(1L, 2);
        
        assertFalse(result.isSuccess());
        assertEquals("Cannot update status of cancelled order", result.getMessage());
    }

    @Test
    @DisplayName("测试 updateStatus - 已评价订单不能更新")
    void testUpdateStatus_CannotUpdateReviewedOrder() {
        setupUserAuthentication();
        Order reviewedOrder = new Order();
        reviewedOrder.setId(1L);
        reviewedOrder.setUserId(1L);
        reviewedOrder.setStatus(5);
        
        when(orderMapper.selectById(1L)).thenReturn(reviewedOrder);
        
        var result = orderController.updateStatus(1L, 2);
        
        assertFalse(result.isSuccess());
        assertEquals("Cannot update status of reviewed order", result.getMessage());
    }

    @Test
    @DisplayName("测试 updateStatus - 普通用户取消订单")
    void testUpdateStatus_UserCancelOrder() {
        setupUserAuthentication();
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(1);
        order.setPayAmount(new BigDecimal("30.00"));
        
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(testOrderItem));
        when(productService.restoreStock(1L, 2)).thenReturn(true);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        when(orderCancelLogMapper.insert(any(OrderCancelLog.class))).thenReturn(1);
        
        var result = orderController.updateStatus(1L, 3);
        
        assertTrue(result.isSuccess());
        verify(productService, times(1)).restoreStock(1L, 2);
        verify(orderCancelLogMapper, times(1)).insert(any(OrderCancelLog.class));
    }

    @Test
    @DisplayName("测试 updateStatus - 普通用户确认收货")
    void testUpdateStatus_UserConfirmDelivery() {
        setupUserAuthentication();
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(2);
        
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        
        var result = orderController.updateStatus(1L, 4);
        
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("测试 updateStatus - 无效的状态转换")
    void testUpdateStatus_InvalidTransition() {
        setupUserAuthentication();
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(1);
        
        when(orderMapper.selectById(1L)).thenReturn(order);
        
        var result = orderController.updateStatus(1L, 4);
        
        assertFalse(result.isSuccess());
        assertEquals("Invalid status transition", result.getMessage());
    }

    @Test
    @DisplayName("测试 updateStatus - 管理员可以进行任何状态转换")
    void testUpdateStatus_AdminCanDoAnyTransition() {
        setupAdminAuthentication();
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(1);
        
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        
        var result = orderController.updateStatus(1L, 4);
        
        assertTrue(result.isSuccess());
    }
}

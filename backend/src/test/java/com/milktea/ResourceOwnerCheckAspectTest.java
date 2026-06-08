package com.milktea;

import com.milktea.annotation.ResourceOwnerCheck;
import com.milktea.annotation.ResourceType;
import com.milktea.aspect.ResourceOwnerCheckAspect;
import com.milktea.common.Result;
import com.milktea.entity.CartItem;
import com.milktea.entity.Order;
import com.milktea.mapper.CartItemMapper;
import com.milktea.mapper.OrderMapper;
import com.milktea.util.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceOwnerCheckAspect 归属校验切面测试")
class ResourceOwnerCheckAspectTest {

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private ResourceOwnerCheckAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private ResourceOwnerCheck resourceOwnerCheck;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    private void setupUserAuth(Long userId) {
        when(authentication.getDetails()).thenReturn(userId);
        when(authentication.getAuthorities()).thenReturn(new ArrayList<>());
    }

    private void setupAdminAuth(Long userId) {
        when(authentication.getDetails()).thenReturn(userId);
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
    }

    private void mockMethodParams(String[] paramNames, Object[] args) throws Exception {
        when(methodSignature.getParameterNames()).thenReturn(paramNames);
        when(joinPoint.getArgs()).thenReturn(args);

        Method mockMethod = ResourceOwnerCheckAspectTest.class.getDeclaredMethod(
                "stubMethod", Long.class);
        when(methodSignature.getMethod()).thenReturn(mockMethod);
    }

    @SuppressWarnings("unused")
    public void stubMethod(Long id) {}

    @Test
    @DisplayName("ORDER - 资源不存在时返回 notFoundMessage")
    void testCheckResourceOwnership_OrderNotFound() throws Throwable {
        setupUserAuth(1L);
        when(resourceOwnerCheck.resourceType()).thenReturn(ResourceType.ORDER);
        when(resourceOwnerCheck.idParam()).thenReturn("id");
        when(resourceOwnerCheck.notFoundMessage()).thenReturn("Order not found");
        when(resourceOwnerCheck.notAuthorizedMessage()).thenReturn("Not authorized");
        when(resourceOwnerCheck.allowAdmin()).thenReturn(true);

        mockMethodParams(new String[]{"id"}, new Object[]{999L});
        when(orderMapper.selectById(999L)).thenReturn(null);

        Object result = aspect.checkResourceOwnership(joinPoint, resourceOwnerCheck);

        assertInstanceOf(Result.class, result);
        Result<?> r = (Result<?>) result;
        assertFalse(r.isSuccess());
        assertEquals("Order not found", r.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("ORDER - 非本人且非管理员时返回 notAuthorizedMessage")
    void testCheckResourceOwnership_OrderNotAuthorized() throws Throwable {
        setupUserAuth(1L);
        when(resourceOwnerCheck.resourceType()).thenReturn(ResourceType.ORDER);
        when(resourceOwnerCheck.idParam()).thenReturn("id");
        when(resourceOwnerCheck.notFoundMessage()).thenReturn("Order not found");
        when(resourceOwnerCheck.notAuthorizedMessage()).thenReturn("Not authorized to view this order");
        when(resourceOwnerCheck.allowAdmin()).thenReturn(true);

        mockMethodParams(new String[]{"id"}, new Object[]{2L});

        Order otherUserOrder = new Order();
        otherUserOrder.setId(2L);
        otherUserOrder.setUserId(999L);
        when(orderMapper.selectById(2L)).thenReturn(otherUserOrder);

        Object result = aspect.checkResourceOwnership(joinPoint, resourceOwnerCheck);

        assertInstanceOf(Result.class, result);
        Result<?> r = (Result<?>) result;
        assertFalse(r.isSuccess());
        assertEquals("Not authorized to view this order", r.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("ORDER - 资源属于当前用户时放行")
    void testCheckResourceOwnership_OrderOwnerAllowed() throws Throwable {
        setupUserAuth(1L);
        when(resourceOwnerCheck.resourceType()).thenReturn(ResourceType.ORDER);
        when(resourceOwnerCheck.idParam()).thenReturn("id");
        when(resourceOwnerCheck.notFoundMessage()).thenReturn("Order not found");
        when(resourceOwnerCheck.notAuthorizedMessage()).thenReturn("Not authorized");
        when(resourceOwnerCheck.allowAdmin()).thenReturn(true);

        mockMethodParams(new String[]{"id"}, new Object[]{1L});

        Order ownOrder = new Order();
        ownOrder.setId(1L);
        ownOrder.setUserId(1L);
        when(orderMapper.selectById(1L)).thenReturn(ownOrder);
        when(joinPoint.proceed()).thenReturn(Result.success(ownOrder));

        Object result = aspect.checkResourceOwnership(joinPoint, resourceOwnerCheck);

        assertInstanceOf(Result.class, result);
        Result<?> r = (Result<?>) result;
        assertTrue(r.isSuccess());
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("ORDER - 管理员可访问他人订单（allowAdmin=true）")
    void testCheckResourceOwnership_AdminAllowedForOrder() throws Throwable {
        setupAdminAuth(2L);
        when(resourceOwnerCheck.resourceType()).thenReturn(ResourceType.ORDER);
        when(resourceOwnerCheck.idParam()).thenReturn("id");
        when(resourceOwnerCheck.notFoundMessage()).thenReturn("Order not found");
        when(resourceOwnerCheck.notAuthorizedMessage()).thenReturn("Not authorized");
        when(resourceOwnerCheck.allowAdmin()).thenReturn(true);

        mockMethodParams(new String[]{"id"}, new Object[]{2L});

        Order otherUserOrder = new Order();
        otherUserOrder.setId(2L);
        otherUserOrder.setUserId(999L);
        when(orderMapper.selectById(2L)).thenReturn(otherUserOrder);
        when(joinPoint.proceed()).thenReturn(Result.success(otherUserOrder));

        Object result = aspect.checkResourceOwnership(joinPoint, resourceOwnerCheck);

        assertInstanceOf(Result.class, result);
        Result<?> r = (Result<?>) result;
        assertTrue(r.isSuccess());
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("CART_ITEM - 资源不存在时返回 notFoundMessage")
    void testCheckResourceOwnership_CartItemNotFound() throws Throwable {
        setupUserAuth(1L);
        when(resourceOwnerCheck.resourceType()).thenReturn(ResourceType.CART_ITEM);
        when(resourceOwnerCheck.idParam()).thenReturn("id");
        when(resourceOwnerCheck.notFoundMessage()).thenReturn("Cart item not found");
        when(resourceOwnerCheck.notAuthorizedMessage()).thenReturn("Not authorized");
        when(resourceOwnerCheck.allowAdmin()).thenReturn(false);

        mockMethodParams(new String[]{"id"}, new Object[]{999L});
        when(cartItemMapper.selectById(999L)).thenReturn(null);

        Object result = aspect.checkResourceOwnership(joinPoint, resourceOwnerCheck);

        assertInstanceOf(Result.class, result);
        Result<?> r = (Result<?>) result;
        assertFalse(r.isSuccess());
        assertEquals("Cart item not found", r.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("CART_ITEM - 非本人时返回 notAuthorizedMessage")
    void testCheckResourceOwnership_CartItemNotAuthorized() throws Throwable {
        setupUserAuth(1L);
        when(resourceOwnerCheck.resourceType()).thenReturn(ResourceType.CART_ITEM);
        when(resourceOwnerCheck.idParam()).thenReturn("id");
        when(resourceOwnerCheck.notFoundMessage()).thenReturn("Cart item not found");
        when(resourceOwnerCheck.notAuthorizedMessage()).thenReturn("Not authorized to update this cart item");
        when(resourceOwnerCheck.allowAdmin()).thenReturn(false);

        mockMethodParams(new String[]{"id"}, new Object[]{2L});

        CartItem otherUserItem = new CartItem();
        otherUserItem.setId(2L);
        otherUserItem.setUserId(999L);
        when(cartItemMapper.selectById(2L)).thenReturn(otherUserItem);

        Object result = aspect.checkResourceOwnership(joinPoint, resourceOwnerCheck);

        assertInstanceOf(Result.class, result);
        Result<?> r = (Result<?>) result;
        assertFalse(r.isSuccess());
        assertEquals("Not authorized to update this cart item", r.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("CART_ITEM - 资源属于当前用户时放行")
    void testCheckResourceOwnership_CartItemOwnerAllowed() throws Throwable {
        setupUserAuth(1L);
        when(resourceOwnerCheck.resourceType()).thenReturn(ResourceType.CART_ITEM);
        when(resourceOwnerCheck.idParam()).thenReturn("id");
        when(resourceOwnerCheck.notFoundMessage()).thenReturn("Cart item not found");
        when(resourceOwnerCheck.notAuthorizedMessage()).thenReturn("Not authorized");
        when(resourceOwnerCheck.allowAdmin()).thenReturn(false);

        mockMethodParams(new String[]{"id"}, new Object[]{1L});

        CartItem ownItem = new CartItem();
        ownItem.setId(1L);
        ownItem.setUserId(1L);
        when(cartItemMapper.selectById(1L)).thenReturn(ownItem);
        when(joinPoint.proceed()).thenReturn(Result.success("Updated"));

        Object result = aspect.checkResourceOwnership(joinPoint, resourceOwnerCheck);

        assertInstanceOf(Result.class, result);
        Result<?> r = (Result<?>) result;
        assertTrue(r.isSuccess());
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("CART_ITEM - allowAdmin=false 时管理员也不能访问他人资源")
    void testCheckResourceOwnership_CartItemAdminBlockedWhenAllowAdminFalse() throws Throwable {
        setupAdminAuth(2L);
        when(resourceOwnerCheck.resourceType()).thenReturn(ResourceType.CART_ITEM);
        when(resourceOwnerCheck.idParam()).thenReturn("id");
        when(resourceOwnerCheck.notFoundMessage()).thenReturn("Cart item not found");
        when(resourceOwnerCheck.notAuthorizedMessage()).thenReturn("Not authorized to delete this cart item");
        when(resourceOwnerCheck.allowAdmin()).thenReturn(false);

        mockMethodParams(new String[]{"id"}, new Object[]{2L});

        CartItem otherUserItem = new CartItem();
        otherUserItem.setId(2L);
        otherUserItem.setUserId(999L);
        when(cartItemMapper.selectById(2L)).thenReturn(otherUserItem);

        Object result = aspect.checkResourceOwnership(joinPoint, resourceOwnerCheck);

        assertInstanceOf(Result.class, result);
        Result<?> r = (Result<?>) result;
        assertFalse(r.isSuccess());
        assertEquals("Not authorized to delete this cart item", r.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("自定义 idParam 从方法参数中提取资源ID")
    void testCheckResourceOwnership_CustomIdParam() throws Throwable {
        setupUserAuth(1L);
        when(resourceOwnerCheck.resourceType()).thenReturn(ResourceType.ORDER);
        when(resourceOwnerCheck.idParam()).thenReturn("orderId");
        when(resourceOwnerCheck.notFoundMessage()).thenReturn("Order not found");
        when(resourceOwnerCheck.notAuthorizedMessage()).thenReturn("Not authorized");
        when(resourceOwnerCheck.allowAdmin()).thenReturn(true);

        mockMethodParams(new String[]{"orderId"}, new Object[]{5L});

        Order ownOrder = new Order();
        ownOrder.setId(5L);
        ownOrder.setUserId(1L);
        when(orderMapper.selectById(5L)).thenReturn(ownOrder);
        when(joinPoint.proceed()).thenReturn(Result.success(ownOrder));

        Object result = aspect.checkResourceOwnership(joinPoint, resourceOwnerCheck);

        assertInstanceOf(Result.class, result);
        Result<?> r = (Result<?>) result;
        assertTrue(r.isSuccess());
        verify(joinPoint, times(1)).proceed();
        verify(orderMapper).selectById(5L);
    }

    @Test
    @DisplayName("参数名不匹配时抛出 IllegalArgumentException")
    void testCheckResourceOwnership_ParamNotFound() throws Throwable {
        setupUserAuth(1L);
        when(resourceOwnerCheck.resourceType()).thenReturn(ResourceType.ORDER);
        when(resourceOwnerCheck.idParam()).thenReturn("missingParam");
        when(resourceOwnerCheck.notFoundMessage()).thenReturn("Order not found");
        when(resourceOwnerCheck.notAuthorizedMessage()).thenReturn("Not authorized");
        when(resourceOwnerCheck.allowAdmin()).thenReturn(true);

        mockMethodParams(new String[]{"id"}, new Object[]{1L});

        assertThrows(IllegalArgumentException.class, () ->
                aspect.checkResourceOwnership(joinPoint, resourceOwnerCheck));
    }

    @Test
    @DisplayName("ORDER - allowAdmin=true 且管理员本人的资源也应放行")
    void testCheckResourceOwnership_AdminOwnOrder() throws Throwable {
        setupAdminAuth(1L);
        when(resourceOwnerCheck.resourceType()).thenReturn(ResourceType.ORDER);
        when(resourceOwnerCheck.idParam()).thenReturn("id");
        when(resourceOwnerCheck.notFoundMessage()).thenReturn("Order not found");
        when(resourceOwnerCheck.notAuthorizedMessage()).thenReturn("Not authorized");
        when(resourceOwnerCheck.allowAdmin()).thenReturn(true);

        mockMethodParams(new String[]{"id"}, new Object[]{1L});

        Order ownOrder = new Order();
        ownOrder.setId(1L);
        ownOrder.setUserId(1L);
        when(orderMapper.selectById(1L)).thenReturn(ownOrder);
        when(joinPoint.proceed()).thenReturn(Result.success(ownOrder));

        Object result = aspect.checkResourceOwnership(joinPoint, resourceOwnerCheck);

        assertInstanceOf(Result.class, result);
        Result<?> r = (Result<?>) result;
        assertTrue(r.isSuccess());
        verify(joinPoint, times(1)).proceed();
    }
}

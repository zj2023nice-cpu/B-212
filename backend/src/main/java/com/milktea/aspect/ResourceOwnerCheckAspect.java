package com.milktea.aspect;

import com.milktea.annotation.ResourceOwnerCheck;
import com.milktea.annotation.ResourceType;
import com.milktea.common.Result;
import com.milktea.entity.CartItem;
import com.milktea.entity.Order;
import com.milktea.mapper.CartItemMapper;
import com.milktea.mapper.OrderMapper;
import com.milktea.util.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(0)
public class ResourceOwnerCheckAspect {

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Around("@annotation(resourceOwnerCheck)")
    public Object checkResourceOwnership(ProceedingJoinPoint joinPoint, ResourceOwnerCheck resourceOwnerCheck) throws Throwable {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = resourceOwnerCheck.allowAdmin() && SecurityUtils.isCurrentUserAdmin();

        Long resourceId = extractResourceId(joinPoint, resourceOwnerCheck.idParam());
        Long ownerId = loadResourceOwnerId(resourceOwnerCheck.resourceType(), resourceId, resourceOwnerCheck.notFoundMessage());

        if (ownerId == null) {
            return Result.error(resourceOwnerCheck.notFoundMessage());
        }

        if (!isAdmin && !ownerId.equals(currentUserId)) {
            return Result.error(resourceOwnerCheck.notAuthorizedMessage());
        }

        return joinPoint.proceed();
    }

    private Long extractResourceId(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName) && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }

        java.lang.reflect.Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName) && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }

        throw new IllegalArgumentException("Parameter '" + paramName + "' not found in method " + method.getName());
    }

    private Long loadResourceOwnerId(ResourceType resourceType, Long resourceId, String notFoundMessage) {
        switch (resourceType) {
            case CART_ITEM:
                CartItem cartItem = cartItemMapper.selectById(resourceId);
                return cartItem != null ? cartItem.getUserId() : null;
            case ORDER:
                Order order = orderMapper.selectById(resourceId);
                return order != null ? order.getUserId() : null;
            default:
                throw new IllegalArgumentException("Unsupported resource type: " + resourceType);
        }
    }
}

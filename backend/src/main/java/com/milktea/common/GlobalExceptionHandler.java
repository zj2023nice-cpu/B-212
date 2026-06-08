package com.milktea.common;

import com.milktea.exception.InsufficientStockException;
import com.milktea.exception.StockConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleInsufficientStockException(InsufficientStockException e) {
        logger.warn("库存不足异常: {}", e.getMessage());
        return Result.error(ResultCode.PRODUCT_STOCK_INSUFFICIENT, e.getMessage());
    }

    @ExceptionHandler(StockConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<String> handleStockConflictException(StockConflictException e) {
        logger.warn("库存冲突异常: {}", e.getMessage());
        return Result.error(ResultCode.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("非法参数异常: {}", e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleIllegalStateException(IllegalStateException e) {
        logger.warn("非法状态异常: {}", e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<String> handleDuplicateKeyException(DuplicateKeyException e) {
        logger.warn("数据重复: {}", e.getMessage());
        return Result.error(ResultCode.DUPLICATE_KEY_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("参数校验失败: {}", message);
        return Result.error(ResultCode.UNPROCESSABLE_ENTITY, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("参数绑定失败: {}", message);
        return Result.error(ResultCode.UNPROCESSABLE_ENTITY, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = String.format("缺少必要参数: %s", e.getParameterName());
        logger.warn(message);
        return Result.error(ResultCode.PARAM_MISSING, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数类型错误: 参数名=%s, 期望类型=%s", 
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        logger.warn(message);
        return Result.error(ResultCode.PARAM_TYPE_ERROR, message);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<String> handleNoHandlerFoundException(NoHandlerFoundException e) {
        String message = String.format("请求的资源不存在: %s", e.getRequestURL());
        logger.warn(message);
        return Result.error(ResultCode.NOT_FOUND, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = String.format("不支持的请求方法: %s", e.getMethod());
        logger.warn(message);
        return Result.error(ResultCode.METHOD_NOT_ALLOWED, message);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<String> handleAuthenticationException(AuthenticationException e) {
        logger.warn("认证失败: {}", e.getMessage());
        if (e instanceof BadCredentialsException) {
            return Result.error(ResultCode.USER_PASSWORD_ERROR, "用户名或密码错误");
        }
        return Result.error(ResultCode.UNAUTHORIZED, "认证失败，请重新登录");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<String> handleAccessDeniedException(AccessDeniedException e) {
        logger.warn("权限不足: {}", e.getMessage());
        return Result.error(ResultCode.FORBIDDEN, "没有权限访问此资源");
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleRuntimeException(RuntimeException e) {
        logger.error("运行时异常: ", e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleException(Exception e) {
        logger.error("系统异常: ", e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR, "系统内部错误，请稍后重试");
    }
}

package com.milktea.common;

import com.milktea.exception.BusinessException;
import com.milktea.exception.InsufficientStockException;
import com.milktea.exception.StockConflictException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        logger.warn("业务异常: [{}] {}", errorCode.getCode(), e.getMessage());
        ErrorResponse response = ErrorResponse.of(errorCode, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientStockException(InsufficientStockException e, HttpServletRequest request) {
        logger.warn("库存不足异常: {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.C0003, e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(StockConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleStockConflictException(StockConflictException e, HttpServletRequest request) {
        logger.warn("库存冲突异常: {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.B0050, e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        logger.warn("非法参数异常: {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.D0001, e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalStateException(IllegalStateException e, HttpServletRequest request) {
        logger.warn("非法状态异常: {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.D0001, e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        logger.warn("数据重复: {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.D0010, ErrorCode.D0010.getMessage(), request.getRequestURI(), withStackTrace(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("参数校验失败: {}", message);
        return ErrorResponse.of(ErrorCode.D0006, message, request.getRequestURI());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("参数绑定失败: {}", message);
        return ErrorResponse.of(ErrorCode.D0006, message, request.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = String.format("缺少必要参数: %s", e.getParameterName());
        logger.warn(message);
        return ErrorResponse.of(ErrorCode.D0002, message, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = String.format("参数类型错误: 参数名=%s, 期望类型=%s",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        logger.warn(message);
        return ErrorResponse.of(ErrorCode.D0003, message, request.getRequestURI());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        String message = String.format("请求的资源不存在: %s", e.getRequestURL());
        logger.warn(message);
        return ErrorResponse.of(ErrorCode.D0007, message, request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String message = String.format("不支持的请求方法: %s", e.getMethod());
        logger.warn(message);
        return ErrorResponse.of(ErrorCode.D0008, message, request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        logger.warn("认证失败: {}", e.getMessage());
        if (e instanceof BadCredentialsException) {
            return ErrorResponse.of(ErrorCode.A0005, "用户名或密码错误", request.getRequestURI());
        }
        return ErrorResponse.of(ErrorCode.A0016, "认证失败，请重新登录", request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        logger.warn("权限不足: {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.A0012, "没有权限访问此资源", request.getRequestURI());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        logger.error("运行时异常: ", e);
        return ErrorResponse.of(ErrorCode.D0018, "系统内部错误，请稍后重试", request.getRequestURI(), withStackTrace(e));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception e, HttpServletRequest request) {
        logger.error("系统异常: ", e);
        return ErrorResponse.of(ErrorCode.D0018, "系统内部错误，请稍后重试", request.getRequestURI(), withStackTrace(e));
    }

    private String withStackTrace(Exception e) {
        if (!isDevProfile()) {
            return null;
        }
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private boolean isDevProfile() {
        return activeProfile != null && activeProfile.contains("dev");
    }
}

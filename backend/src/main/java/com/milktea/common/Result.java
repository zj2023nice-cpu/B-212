package com.milktea.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return success(ResultCode.SUCCESS, data);
    }

    public static <T> Result<T> success(ResultCode resultCode, T data) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error() {
        return error(ResultCode.INTERNAL_SERVER_ERROR);
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.INTERNAL_SERVER_ERROR.getCode());
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    public static <T> Result<T> error(ResultCode resultCode, String message) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> badRequest(String message) {
        return error(ResultCode.BAD_REQUEST, message);
    }

    public static <T> Result<T> unauthorized(String message) {
        return error(ResultCode.UNAUTHORIZED, message);
    }

    public static <T> Result<T> forbidden(String message) {
        return error(ResultCode.FORBIDDEN, message);
    }

    public static <T> Result<T> notFound(String message) {
        return error(ResultCode.NOT_FOUND, message);
    }

    public static <T> Result<T> conflict(String message) {
        return error(ResultCode.CONFLICT, message);
    }

    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }

    public boolean isError() {
        return !isSuccess();
    }
}

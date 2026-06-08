package com.milktea.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String errorCode;
    private String errorMessage;
    private LocalDateTime timestamp;
    private String path;
    private String stackTrace;

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return of(errorCode, errorCode.getMessage(), path, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String customMessage, String path) {
        return of(errorCode, customMessage, path, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String customMessage, String path, String stackTrace) {
        ErrorResponse response = new ErrorResponse();
        response.setErrorCode(errorCode.getCode());
        response.setErrorMessage(customMessage != null ? customMessage : errorCode.getMessage());
        response.setTimestamp(LocalDateTime.now());
        response.setPath(path);
        response.setStackTrace(stackTrace);
        return response;
    }
}

package com.logilink.eureka.client.gateway.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException{

    private final HttpStatus status;
    private final String code;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.status = errorCode.getStatus();
    }

    protected AppException(HttpStatus httpStatus, String code, String message) {
        super(message);
        this.code = code;
        this.status = httpStatus;
    }

    public static AppException of(ErrorCode errorCode) {
        return new AppException(errorCode);
    }
}
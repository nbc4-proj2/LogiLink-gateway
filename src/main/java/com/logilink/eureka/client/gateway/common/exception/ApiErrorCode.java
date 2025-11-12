package com.logilink.eureka.client.gateway.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ApiErrorCode implements ErrorCode {
    INVALID_REQUEST("API001", "잘못된 요청입니다.", HttpStatus.FORBIDDEN),
    FORBIDDEN("API002", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    UNAUTHORIZED("API003", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED)
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;

    ApiErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
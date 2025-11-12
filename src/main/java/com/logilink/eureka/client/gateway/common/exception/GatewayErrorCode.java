package com.logilink.eureka.client.gateway.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GatewayErrorCode implements  ErrorCode{
    TOKEN_IS_NOT_EXISTING_OR_INVALID("GATEWAY0001", "토큰이 없거나 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    FAILED_TOKEN_VALIDATION("GATEWAY0002", "토큰 검증에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    REQUIRED_DATA_IS_NULL("GATEWAY0003", "토큰에 필수 데이터가 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
    EXPIRED_TOKEN("GATEWAY0004", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED)
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;

    GatewayErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}

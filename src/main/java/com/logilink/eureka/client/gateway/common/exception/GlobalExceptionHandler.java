package com.logilink.eureka.client.gateway.common.exception;

import com.sparta.logilinkcommon.common.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<BaseResponse> handleException(AppException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(BaseResponse.fail(exception));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleException(
            MethodArgumentNotValidException exception) {
        List<ErrorResponse.ErrorField> errorFields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ErrorResponse.ErrorField(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ApiErrorCode.INVALID_REQUEST, errorFields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleException() {
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.from(ApiErrorCode.INVALID_REQUEST));
    }
}
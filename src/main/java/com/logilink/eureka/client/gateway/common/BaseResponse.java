package com.logilink.eureka.client.gateway.common;


import com.logilink.eureka.client.gateway.common.exception.AppException;
import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class BaseResponse<T> {

    private boolean isSuccess;
    private String message;
    private HttpStatus status;
    private T result;

//    // 요청에 성공한 경우
//    private BaseResponse(T result) {
//        this.isAuccess = true;
//        this.message = "요청에 성공했습니다.";
//        this.status = HttpStatus.OK;
//        this.result = result;
//    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(true, "요청이 성공했습니다.", HttpStatus.OK, data);
    }

    public static BaseResponse<AppException> fail(AppException exception) {
        return new BaseResponse<>(false, "요청이 실패했습니다.", exception.getStatus(), exception);
    }
}

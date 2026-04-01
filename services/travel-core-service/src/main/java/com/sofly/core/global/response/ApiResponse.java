package com.sofly.core.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sofly.core.global.response.code.BaseErrorCode;
import com.sofly.core.global.response.code.BaseSuccessCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"success", "code", "message", "data"})
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {
    // 성공 1: 데이터만
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, null, data);
    }

    // 성공 2: enum 코드 + 데이터
    public static <T> ApiResponse<T> success(BaseSuccessCode code, T data) {
        return new ApiResponse<>(true, null, code.getMessage(), data);
    }

    // 실패 1: 메시지만 (빠른 사용용)
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    // 실패 2: ErrorStatus enum 사용 (권장)
    public static <T> ApiResponse<T> fail(BaseErrorCode code) {
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), null);
    }

    // 실패 3: ErrorStatus enum + 커스텀 메시지
    public static <T> ApiResponse<T> fail(BaseErrorCode code, String customMessage) {
        return new ApiResponse<>(false, code.getCode(), customMessage, null);
    }
}

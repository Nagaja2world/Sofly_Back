package com.sofly.core.global.response.exception;

import com.sofly.core.global.response.code.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {
    private final BaseErrorCode code;
    private final String customMessage;


    // 기본 메시지 사용 case를 위한 생성자 정의
    public GeneralException(BaseErrorCode code) {
        this.code = code;
        this.customMessage = code.getMessage();
    }
}

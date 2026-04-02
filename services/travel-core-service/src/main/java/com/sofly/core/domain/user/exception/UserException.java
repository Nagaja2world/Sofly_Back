package com.sofly.core.domain.user.exception;

import com.sofly.core.global.response.code.BaseErrorCode;

import lombok.Getter;

@Getter
public class UserException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public UserException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public UserException(BaseErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}

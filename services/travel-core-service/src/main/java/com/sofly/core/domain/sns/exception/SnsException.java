package com.sofly.core.domain.sns.exception;

import com.sofly.core.global.response.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class SnsException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public SnsException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

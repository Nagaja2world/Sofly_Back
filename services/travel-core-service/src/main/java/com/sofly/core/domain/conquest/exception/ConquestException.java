package com.sofly.core.domain.conquest.exception;

import com.sofly.core.global.response.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class ConquestException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public ConquestException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

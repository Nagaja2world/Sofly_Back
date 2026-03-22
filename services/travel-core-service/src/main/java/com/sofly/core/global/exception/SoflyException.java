package com.sofly.core.global.exception;

import lombok.Getter;

@Getter
public class SoflyException extends RuntimeException {

    private final ErrorCode errorCode;

    public SoflyException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SoflyException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}

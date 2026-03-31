package com.sofly.core.global.auth.exception;

import com.sofly.core.global.response.code.BaseErrorCode;
import com.sofly.core.global.response.exception.GeneralException;

public class AuthException extends GeneralException {
    public AuthException(BaseErrorCode code) {
        super(code);
    }
}

package com.sofly.core.domain.workspace.exception;

import com.sofly.core.global.response.code.BaseErrorCode;

import lombok.Getter;

@Getter
public class WorkspaceException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public WorkspaceException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public WorkspaceException(BaseErrorCode errorCode, String customMessage){
        super(customMessage);
        this.errorCode = errorCode;
    }
}

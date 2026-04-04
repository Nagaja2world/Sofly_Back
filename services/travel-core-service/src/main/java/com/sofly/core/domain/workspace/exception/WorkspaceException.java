package com.sofly.core.domain.workspace.exception;

import com.sofly.core.domain.workspace.code.WorkspaceErrorCode;
import lombok.Getter;

@Getter
public class WorkspaceException extends RuntimeException {

    private final WorkspaceErrorCode errorCode;

    public WorkspaceException(WorkspaceErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

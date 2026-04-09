package com.sofly.core.domain.workspace.code;

import com.sofly.core.global.response.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WorkspaceErrorCode implements BaseErrorCode {

    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKSPACE_001", "워크스페이스를 찾을 수 없습니다."),
    WORKSPACE_FORBIDDEN(HttpStatus.FORBIDDEN, "WORKSPACE_002", "워크스페이스에 대한 권한이 없습니다."),
    ALREADY_WORKSPACE_MEMBER(HttpStatus.CONFLICT, "WORKSPACE_003", "이미 워크스페이스 멤버입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKSPACE_004", "멤버를 찾을 수 없습니다."),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "WORKSPACE_005", "유효하지 않은 초대 코드입니다."),
    OWNER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "WORKSPACE_006", "워크스페이스 소유자는 탈퇴할 수 없습니다."),
    CANNOT_ASSIGN_OWNER_ROLE(HttpStatus.BAD_REQUEST, "WORKSPACE_007", "소유자 역할은 직접 변경할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

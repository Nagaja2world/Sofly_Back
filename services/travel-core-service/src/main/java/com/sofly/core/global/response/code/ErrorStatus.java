package com.sofly.core.global.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 공통
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 오류가 발생했습니다."),

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 요청입니다."),
    // 인증
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),

    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_004", "Refresh Token을 찾을 수 없습니다."),

    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_005", "Refresh Token이 만료되었습니다."),

    // 400 Bad Request: 클라이언트의 요청이 잘못됨
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),

    // 403 Forbidden: 인증은 됐으나 해당 리소스에 권한이 없음
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "제한된 접근입니다."),

    // 404 Not Found: 요청한 리소스를 찾을 수 없음
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "해당 리소스를 찾을 수 없습니다."),

    // 409 Conflict: 서버의 현재 상태와 요청이 충돌함
    CONFLICT(HttpStatus.CONFLICT, "COMMON409", "데이터 충돌이 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

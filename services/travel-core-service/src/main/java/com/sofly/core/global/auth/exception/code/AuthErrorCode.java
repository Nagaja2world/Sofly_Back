package com.sofly.core.global.auth.exception.code;

import org.springframework.http.HttpStatus;

import com.sofly.core.global.response.code.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    INCORRECT_PASSWORD(HttpStatus.UNAUTHORIZED,"INCORRECT_PASSWORD", "비밀번호가 틀렸습니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,"INVALID_REFRESH_TOKEN","유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED,"REFRESH_TOKEN_EXPIRED","리프레시 토큰이 만료되었습니다. 다시 로그인해 주세요.")
    ,
    // 회원가입 시 중복된 아이디가 있을 경우
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "DUPLICATE_LOGIN_ID", "이미 존재하는 아이디입니다."),
    EMPTY_AUTHENTICATION(HttpStatus.UNAUTHORIZED, "EMPTY_AUTHENTICATION", "인증 정보가 존재하지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "해당 사용자를 찾지 못했습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

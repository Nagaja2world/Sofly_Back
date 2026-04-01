package com.sofly.core.domain.user.code;

import org.springframework.http.HttpStatus;

import com.sofly.core.global.response.code.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    LOGIN_ID_REQUIRED(HttpStatus.BAD_REQUEST,"LOGIN_ID_REQUIRED","로그인 ID는 필수 입력값입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "해당 사용자를 찾지 못했습니다."),
    LOGIN_ID_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "ID_ALREADY_EXIST","이미 존재하는 ID입니다"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST,"INVALID_REQUEST","요청이 유효하지 않습니다")
    ;


    private final HttpStatus status;
    private final String code;
    private final String message;
}

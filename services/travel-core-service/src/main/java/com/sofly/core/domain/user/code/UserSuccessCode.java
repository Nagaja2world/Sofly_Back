package com.sofly.core.domain.user.code;

import org.springframework.http.HttpStatus;

import com.sofly.core.global.response.code.BaseSuccessCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserSuccessCode implements BaseSuccessCode {

    FOUND(HttpStatus.OK, "MEMBER_FOUND", "성공적으로 사용자를 조회했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}

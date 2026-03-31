
package com.sofly.core.global.auth.exception.code;

import org.springframework.http.HttpStatus;

import com.sofly.core.global.response.code.BaseSuccessCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthSuccessCode implements BaseSuccessCode {
    LOGIN_SUCCESS(HttpStatus.OK, "LOGIN_SUCCESS", "로그인에 성공하였습니다."),
    SIGNUP_SUCCESS(HttpStatus.CREATED, "SIGNUP_SUCCESS", "회원가입이 완료되었습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK,"LOGOUT_SUCCESS","로그아웃에 성공하였습니다."),
    REISSUE_SUCCESS(HttpStatus.CREATED, "REISSUE_SUCCESS", "재발급에 성공하였습니다.")
;
    private final HttpStatus status;
    private final String code;
    private final String message;
}

package com.sofly.core.domain.user.code;

import org.springframework.http.HttpStatus;

import com.sofly.core.global.response.code.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    PROFILE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_002", "프로필 업데이트에 실패했습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}

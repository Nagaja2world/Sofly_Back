package com.sofly.core.global.response.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessStatus implements BaseSuccessCode {

    // 200 OK: 요청이 성공적으로 처리됨 (기본)
    _OK(HttpStatus.OK, "COMMON200", "요청에 성공했습니다."),

    // 201 Created: 새 리소스가 성공적으로 생성됨
    _CREATED(HttpStatus.CREATED, "COMMON201", "리소스가 성공적으로 생성되었습니다."),

    // 204 No Content
    _NO_CONTENT(HttpStatus.NO_CONTENT, "COMMON204", "성공적으로 처리되었으나 본문이 비어있습니다.");
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}

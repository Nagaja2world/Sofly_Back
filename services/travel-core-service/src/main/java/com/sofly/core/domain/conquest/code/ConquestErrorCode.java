package com.sofly.core.domain.conquest.code;

import com.sofly.core.global.response.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ConquestErrorCode implements BaseErrorCode {

    COUNTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "CONQUEST_001", "방문 국가 정보를 찾을 수 없습니다."),
    CITY_NOT_FOUND(HttpStatus.NOT_FOUND, "CONQUEST_002", "방문 도시 정보를 찾을 수 없습니다."),
    CONQUEST_FORBIDDEN(HttpStatus.FORBIDDEN, "CONQUEST_003", "해당 정복 정보에 접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

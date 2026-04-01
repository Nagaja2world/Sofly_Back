package com.sofly.core.global.response.code;

import org.springframework.http.HttpStatus;

public interface BaseSuccessCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}

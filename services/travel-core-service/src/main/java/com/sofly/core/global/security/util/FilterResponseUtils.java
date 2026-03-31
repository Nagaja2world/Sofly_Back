package com.sofly.core.global.security.util;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofly.core.global.response.ApiResponse;  // sofly 패키지에 맞게
import com.sofly.core.global.response.code.BaseErrorCode;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FilterResponseUtils {

    private final ObjectMapper objectMapper;

    public void sendUnauthorized(HttpServletResponse response, BaseErrorCode errorCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // sofly ApiResponse 구조에 맞게 수정 필요
        ApiResponse<Object> errorResponse = ApiResponse.fail(errorCode);
        String json = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
    }
}

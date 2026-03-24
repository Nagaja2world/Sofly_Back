package com.sofly.core.global.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(

        @Schema(description = "새로 발급된 Access Token")
        String accessToken,

        @Schema(description = "새로 발급된 Refresh Token")
        String refreshToken
) {
    public static TokenResponse of(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken);
    }
}

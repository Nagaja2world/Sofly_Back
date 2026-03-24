package com.sofly.core.global.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(description = "Refresh Token", example = "eyJhbGci...")
        @NotBlank String refreshToken
) {}

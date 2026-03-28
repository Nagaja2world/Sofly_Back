package com.sofly.core.global.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofly.core.global.auth.dto.RefreshTokenRequest;
import com.sofly.core.global.auth.dto.TokenResponse;
import com.sofly.core.global.auth.service.AuthService;
import com.sofly.core.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Access Token 재발급
     * POST /api/auth/refresh
     */
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token을 재발급합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 로그아웃 (Refresh Token 삭제)
     * POST /api/auth/logout
     */
    @Operation(summary = "로그아웃", description = "Refresh Token을 삭제합니다. Access Token이 필요합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Access Token 없음 또는 만료")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Long userId) {

        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

package com.sofly.core.global.auth.controller;

import com.sofly.core.global.auth.dto.RefreshTokenRequest;
import com.sofly.core.global.auth.dto.TokenResponse;
import com.sofly.core.global.auth.service.AuthService;
import com.sofly.core.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Access Token 재발급
     * POST /api/auth/refresh
     */
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
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Long userId) {

        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

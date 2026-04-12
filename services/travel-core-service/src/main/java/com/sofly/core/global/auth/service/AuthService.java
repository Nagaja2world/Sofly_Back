package com.sofly.core.global.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofly.core.global.auth.dto.RefreshTokenRequest;
import com.sofly.core.global.auth.dto.TokenResponse;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import com.sofly.core.global.security.jwt.JwtProperties;
import com.sofly.core.global.security.jwt.JwtTokenProvider;
import com.sofly.core.global.security.oauth2.RefreshToken;
import com.sofly.core.global.security.oauth2.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    // ── Access Token 재발급 ───────────────────────────────
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        // 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new SoflyException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // refresh 메서드 — refreshToken 값으로 userId 꺼내기
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // Redis에서 조회
        RefreshToken saved = refreshTokenRepository.findById(String.valueOf(userId))
            .orElseThrow(() -> new SoflyException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 저장된 토큰값이랑 요청 토큰값 일치 확인
        if (!saved.getRefreshToken().equals(refreshToken)) {
            throw new SoflyException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        // 새 토큰 발급
        String newAccessToken  = jwtTokenProvider.generateAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // Refresh Token 교체 (Rotate)
        refreshTokenRepository.delete(saved);
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .id(String.valueOf(userId))
                        .refreshToken(newRefreshToken)
                        .expiration(jwtProperties.expiration().refresh().getSeconds())
                        .build()
        );

        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    // ── 로그아웃 ─────────────────────────────────────────
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteById(String.valueOf(userId));
        log.info("OAuth2 로그아웃 - userId: {}", userId);
    }
}

package com.sofly.core.global.auth.service;

import com.sofly.core.global.auth.dto.RefreshTokenRequest;
import com.sofly.core.global.auth.dto.TokenResponse;
import com.sofly.core.global.exception.SoflyException;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.security.jwt.JwtProperties;
import com.sofly.core.global.security.jwt.JwtTokenProvider;
import com.sofly.core.global.security.oauth2.RefreshToken;
import com.sofly.core.global.security.oauth2.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Redis에서 조회
        RefreshToken saved = refreshTokenRepository.findById(refreshToken)
                .orElseThrow(() -> new SoflyException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        Long userId = saved.getUserId();

        // 새 토큰 발급
        String newAccessToken  = jwtTokenProvider.generateAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // Refresh Token 교체 (Rotate)
        refreshTokenRepository.delete(saved);
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .refreshToken(newRefreshToken)
                        .userId(userId)
                        .expiration(jwtProperties.expiration().refresh() / 1000)
                        .build()
        );

        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    // ── 로그아웃 ─────────────────────────────────────────
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}

package com.sofly.core.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    @Autowired
    public JwtTokenProvider(JwtProperties jwtProperties){
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(
            jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getSigningKey() {
        return secretKey;
    }

    // ── Access Token 생성 ────────────────────────────────
    public String generateAccessToken(Long userId) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.expiration().access())))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Refresh Token 생성 ───────────────────────────────
    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.expiration().refresh())))
                .signWith(getSigningKey())
                .compact();
    }

    // ── userId 추출 ──────────────────────────────────────
    public Long getUserId(String token) {
        return Long.parseLong(
                parseClaims(token).getSubject()
        );
    }

    // 단순 토큰 유효 유무 확인 함수
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("유효하지 않은 토큰: {}", e.getMessage());
            return false;
        }
    }

    // ── 토큰 유효성 검증 ─────────────────────────────────
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT 토큰: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("잘못된 JWT 토큰: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있음: {}", e.getMessage());
        }
        return false;
    }

    // ── Claims 파싱 ──────────────────────────────────────
    private Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .clockSkewSeconds(30) // 시간 오차 허용 (네트워크 지연 등 대비)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

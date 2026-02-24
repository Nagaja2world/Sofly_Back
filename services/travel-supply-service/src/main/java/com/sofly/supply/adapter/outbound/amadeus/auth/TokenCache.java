package com.sofly.supply.adapter.outbound.amadeus.auth;

import java.time.Instant;
import java.util.Optional;

public class TokenCache {
    private volatile String token;
    private volatile Instant expiresAt;

    public Optional<String> getValidToken() {
        if (token == null || expiresAt == null) return Optional.empty();
        // 만료 30초 전이면 재발급
        if (Instant.now().isAfter(expiresAt.minusSeconds(30))) return Optional.empty();
        return Optional.of(token);
    }

    public void set(String token, long expiresInSeconds) {
        this.token = token;
        this.expiresAt = Instant.now().plusSeconds(expiresInSeconds);
    }
}
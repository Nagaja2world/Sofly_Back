package com.sofly.core.global.security.jwt;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt.token")
public record JwtProperties(
        String secretKey,
        Expiration expiration
) {
    public record Expiration(
            Duration access,
            Duration refresh
    ) {}
}

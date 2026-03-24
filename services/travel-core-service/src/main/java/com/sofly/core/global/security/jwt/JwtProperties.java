package com.sofly.core.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt.token")
public record JwtProperties(
        String secretKey,
        Expiration expiration
) {
    public record Expiration(
            long access,
            long refresh
    ) {}
}

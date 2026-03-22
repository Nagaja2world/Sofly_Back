package com.sofly.core.global.security.oauth2;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Builder
@RedisHash("refresh_token")
public class RefreshToken {

    @Id
    private String refreshToken;    // key: refresh_token:{refreshToken}

    @Indexed
    private Long userId;            // userId로 검색 가능

    @TimeToLive
    private Long expiration;        // TTL (초 단위) — JwtProperties.expiration.refresh / 1000
}

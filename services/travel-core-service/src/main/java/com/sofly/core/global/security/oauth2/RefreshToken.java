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
    private String id;          // key를 "refresh_token:{userId}" 로 고정

    private String refreshToken; // 실제 토큰 값

    @TimeToLive
    private Long expiration;    // TTL (초)
}

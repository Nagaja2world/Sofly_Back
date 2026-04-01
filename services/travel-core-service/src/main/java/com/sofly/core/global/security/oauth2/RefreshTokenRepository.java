package com.sofly.core.global.security.oauth2;

import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    // findByUserId 제거 — userId를 @Id로 쓰므로 findById로 조회
}

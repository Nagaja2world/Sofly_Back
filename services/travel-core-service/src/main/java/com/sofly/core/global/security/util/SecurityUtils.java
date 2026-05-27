package com.sofly.core.global.security.util;

import com.sofly.core.global.auth.exception.code.AuthErrorCode;
import com.sofly.core.global.auth.exception.AuthException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    /**
     * SecurityContext에서 현재 로그인한 userId 추출
     * JwtAuthenticationFilter에서 principal로 userId(Long)를 저장한 구조 기반
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(AuthErrorCode.EMPTY_AUTHENTICATION);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Long) {
            return (Long) principal;
        }

        // JwtAuthenticationFilter에서 userId를 String으로 넣었을 경우 대비
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                throw new AuthException(AuthErrorCode.INVALID_TOKEN);
            }
        }

        throw new AuthException(AuthErrorCode.EMPTY_AUTHENTICATION);
    }

    public static Long tryGetCurrentUserId() {
        try {
            return getCurrentUserId();
        } catch (AuthException e) {
            return null;
        }
    }
}

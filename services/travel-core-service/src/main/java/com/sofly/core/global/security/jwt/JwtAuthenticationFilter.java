package com.sofly.core.global.security.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sofly.core.global.auth.exception.code.AuthErrorCode;
import com.sofly.core.global.security.util.FilterResponseUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final FilterResponseUtils filterResponseUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        // TODO: social login jwt PR #16 성능개선 필요 (gemini-code-assist)

        // if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
        if (StringUtils.hasText(token)) {
            if (!jwtTokenProvider.validateToken(token)) {
                // 유효하지 않은 토큰 → 즉시 401 반환, 필터 체인 중단
                filterResponseUtils.sendUnauthorized(response, AuthErrorCode.INVALID_TOKEN);
                return; // ← 핵심
            }

            Long userId = jwtTokenProvider.getUserId(token);

            // SecurityContext에 인증 정보 저장
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    // Authorization: Bearer {token} 에서 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

package com.sofly.core.global.security.oauth2;

import com.sofly.core.global.security.jwt.JwtProperties;
import com.sofly.core.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${sofly.oauth2.redirect-uri}")
    private String redirectUri;     // 예: http://localhost:3000/oauth2/callback

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Long userId = oAuth2User.getUserId();

        // 토큰 발급
        String accessToken  = jwtTokenProvider.generateAccessToken(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // 저장 시 id를 userId로 고정
        refreshTokenRepository.deleteById(String.valueOf(userId));
        refreshTokenRepository.save(
            RefreshToken.builder()
                .id(String.valueOf(userId))        // key: refresh_token:{userId}
                .refreshToken(refreshToken)
                .expiration(jwtProperties.expiration().refresh() / 1000)
                .build()
        );

        // 프론트로 redirect (쿼리 파라미터로 토큰 전달)
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        log.info("OAuth2 로그인 성공 - userId: {}", userId);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}

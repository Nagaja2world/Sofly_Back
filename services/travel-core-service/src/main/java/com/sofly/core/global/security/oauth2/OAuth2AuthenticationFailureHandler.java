package com.sofly.core.global.security.oauth2;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${sofly.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String errorMessage = (exception instanceof OAuth2AuthenticationException oauthEx)
                ? oauthEx.getError().getDescription()
                : "인증 처리 중 오류가 발생했습니다.";
        if (errorMessage == null) errorMessage = "알 수 없는 인증 오류가 발생했습니다.";

        log.error("OAuth2 로그인 실패: {}", errorMessage);
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", errorMessage)
                .build()
                .encode()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}

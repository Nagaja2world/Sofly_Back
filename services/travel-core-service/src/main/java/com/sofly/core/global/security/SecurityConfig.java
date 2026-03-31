package com.sofly.core.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.sofly.core.global.auth.exception.code.AuthErrorCode;
import com.sofly.core.global.security.jwt.JwtAuthenticationFilter;
import com.sofly.core.global.security.jwt.JwtTokenProvider;
import com.sofly.core.global.security.oauth2.CustomOAuth2UserService;
import com.sofly.core.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sofly.core.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.sofly.core.global.security.util.FilterResponseUtils;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final FilterResponseUtils filterResponseUtils;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        configureCommonSecurity(http);
        http
            // 요청 권한 설정
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/",
                            "/index.html",
                            "/login/oauth2/**",
                            "/oauth2/**",
                            "/api/auth/**",
                            "/actuator/health",
                            "/swagger-ui/**",
                            "/core-docs",
                            "/v3/api-docs/**"     
                    ).permitAll()
                    .anyRequest().authenticated()
            );
        return http.build();

    }

    private void configureCommonSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                // JWT 필터 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                )
                // 토큰 없이 authenticated() 경로 접근 시 401
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        filterResponseUtils.sendUnauthorized(response,AuthErrorCode.EMPTY_AUTHENTICATION);
                    })
                );
    }

}

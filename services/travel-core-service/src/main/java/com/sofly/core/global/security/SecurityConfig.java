package com.sofly.core.global.security;

import com.sofly.core.global.security.jwt.JwtAuthenticationFilter;
import com.sofly.core.global.security.jwt.JwtTokenProvider;
import com.sofly.core.global.security.oauth2.CustomOAuth2UserService;
import com.sofly.core.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.sofly.core.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용)
            .csrf(AbstractHttpConfigurer::disable)

            // 폼 로그인 비활성화 (소셜 로그인만 사용)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            // 세션 비활성화 (JWT Stateless)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 요청 권한 설정
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/login/oauth2/**",
                            "/oauth2/**",
                            "/api/auth/**",
                            "/actuator/health"
                    ).permitAll()
                    .anyRequest().authenticated()
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
            );

        return http.build();
    }
}

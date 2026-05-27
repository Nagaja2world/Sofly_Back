package com.sofly.core.global.security;

import java.util.List;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                .requestMatchers(
                        "/",
                        "/error",
                        "/index.html",
                        "/user-profile-test.html",
                        "/conquest-test.html",
                        "/websocket-chat.html",
                        "/websocket-chat-test.html",   // TODO: 채팅 REST API 허용 (개발 중에만, 나중에 인증 붙이면 제거
                        "/ws/**",                       // TODO: Websocket 핸드셰이크 경로 허용
                        "/api/v1/messaging/**",
                        "/api/v1/flights/**",
                        "/api/v1/hotels/**",
                        "/login/oauth2/**",
                        "/oauth2/**",
                        "/api/auth/refresh",
                        "/actuator/**",
                        "/actuator/health",
                        "/core/swagger-ui/**",
                        "/core/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/sns/workspaces/search",
                        "/api/sns/workspaces/*/comments",
                        "/api/sns/users/*/profile",
                        "/api/sns/users/*/follow-stats"
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
                        new JwtAuthenticationFilter(jwtTokenProvider, filterResponseUtils),
                        UsernamePasswordAuthenticationFilter.class
                )
                // 토큰 없이 authenticated() 경로 접근 시 401, 권한 없는 경우 403
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        filterResponseUtils.sendUnauthorized(response, AuthErrorCode.EMPTY_AUTHENTICATION);
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        filterResponseUtils.sendUnauthorized(response, AuthErrorCode.ACCESS_DENIED);
                    })
                );

        return http.build();

    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "https://sofly.co.kr"
                // 운영 프론트 도메인 확정 후 추가
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Refresh-Token", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void configureCommonSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        
    }

}

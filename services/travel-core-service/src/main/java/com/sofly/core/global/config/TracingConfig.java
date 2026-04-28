package com.sofly.core.global.config;

import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

import io.micrometer.observation.ObservationRegistry;

@Configuration
public class TracingConfig {

    @Bean
    public ObservationRegistryCustomizer<ObservationRegistry> noActuatorObservations() {
        return registry -> registry.observationConfig()
            .observationPredicate((name, context) -> {
                // /actuator 제외
                if (context instanceof ServerRequestObservationContext ctx) {
                    String uri = ctx.getCarrier().getRequestURI();
                    if (uri.startsWith("/actuator")) return false;
                    // 정적 파일 요청 제외 (폰트, 이미지 등)
                    if (uri.startsWith("/assets")) return false;
                    if (uri.endsWith(".woff") || uri.endsWith(".woff2")
                        || uri.endsWith(".js") || uri.endsWith(".css")) return false;
                }
                // spring.security 제외
                if (name.startsWith("spring.security.")) return false;
                // 스케줄러 제외
                if (name.startsWith("spring.scheduling")) return false;
                // DB/Redis 커넥션 제외
                if (name.startsWith("db.client") || name.equals("connection")) return false;

                return true;
            });
    }
}

package com.sofly.core.global.config;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import io.micrometer.observation.ObservationRegistry;

@Configuration
public class TracingConfig {

    // 기존 ObservationPredicate (HTTP 필터용 - 유지)
    @Bean
    public ObservationRegistryCustomizer<ObservationRegistry> noActuatorObservations() {
        return registry -> registry.observationConfig()
            .observationPredicate((name, context) -> {
                if (context instanceof ServerRequestObservationContext ctx) {
                    String uri = ctx.getCarrier().getRequestURI();
                    if (uri.startsWith("/actuator")) return false;
                    if (uri.startsWith("/assets")) return false;
                    if (uri.endsWith(".woff") || uri.endsWith(".woff2")
                        || uri.endsWith(".js") || uri.endsWith(".css")) return false;
                }
                if (name.startsWith("spring.security.")) return false;
                if (name.startsWith("spring.scheduling")) return false;
                if (name.startsWith("db.client") || name.equals("connection")) return false;
                return true;
            });
    }

    // Lettuce span 필터링 (Brave/Zipkin용)
    @Bean
    public SpanHandler lettuceSpanFilter() {
        return new SpanHandler() {
            @Override
            public boolean begin(TraceContext context, MutableSpan span, TraceContext parent) {
                return !shouldFilter(span.name());
            }

            @Override
            public boolean end(TraceContext context, MutableSpan span, Cause cause) {
                return !shouldFilter(span.name());
            }

            private boolean shouldFilter(String name) {
                if (name == null) return false;
                if (name.equals("connection")) return true;
                if (name.startsWith("task redis-flight-departure-scheduler")) return true;
                return false;
            }
        };
    }
}

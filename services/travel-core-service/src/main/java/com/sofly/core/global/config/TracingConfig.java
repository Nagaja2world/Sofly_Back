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
                if (context instanceof ServerRequestObservationContext ctx) {
                    return !ctx.getCarrier().getRequestURI().startsWith("/actuator");
                }
                return true;
            });
    }
}

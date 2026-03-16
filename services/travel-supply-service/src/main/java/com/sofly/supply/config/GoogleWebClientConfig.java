package com.sofly.supply.config;

import com.sofly.supply.adapter.outbound.google.GooglePlacesProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(GooglePlacesProperties.class)
public class GoogleWebClientConfig {

    @Bean("googlePlacesWebClient")
    public WebClient googlePlacesWebClient(GooglePlacesProperties props) {
        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }
}

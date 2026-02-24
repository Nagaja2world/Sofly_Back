package com.sofly.supply.config;

import com.sofly.supply.adapter.outbound.amadeus.AmadeusProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AmadeusProperties.class)
public class WebClientConfig {

    @Bean
    public WebClient amadeusWebClient(AmadeusProperties props) {
        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }
}
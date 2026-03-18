package com.sofly.supply.config;

import com.sofly.supply.adapter.outbound.amadeus.AmadeusProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AmadeusProperties.class)
public class WebClientConfig {

    // Amadeus 호텔 목록 응답이 클 수 있으므로 버퍼를 10MB로 확장
    private static final int BUFFER_SIZE = 10 * 1024 * 1024;

    @Bean
    public WebClient amadeusWebClient(AmadeusProperties props) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(BUFFER_SIZE))
                .build();

        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .exchangeStrategies(strategies)
                .build();
    }
}
package com.sofly.supply.config;

import com.sofly.supply.adapter.outbound.rapidapi.RapidApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(RapidApiProperties.class)
public class WebClientConfig {

    private static final int BUFFER_SIZE = 10 * 1024 * 1024;

    @Bean("rapidApiWebClient")
    public WebClient rapidApiWebClient(WebClient.Builder builder, RapidApiProperties props){
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(BUFFER_SIZE))
                .build();

        return builder
                .baseUrl(props.baseUrl())
                .defaultHeader("X-RapidAPI-Key", props.apiKey())
                .defaultHeader("X-RapidAPI-Host", props.host())
                .exchangeStrategies(strategies)
                .build();
    }
}

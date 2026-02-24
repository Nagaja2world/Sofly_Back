package com.sofly.supply.adapter.outbound.amadeus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amadeus")
public record AmadeusProperties(
        String baseUrl,
        String clientId,
        String clientSecret
) {}
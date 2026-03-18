package com.sofly.supply.adapter.outbound.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.places")
public record GooglePlacesProperties(
        String apiKey,
        String baseUrl
) {}

package com.sofly.supply.adapter.outbound.rapidapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rapidapi")
public record RapidApiProperties(String apiKey, String host, String baseUrl) {

}

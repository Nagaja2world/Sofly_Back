package com.sofly.supply.adapter.outbound.amadeus.auth;

import com.sofly.supply.adapter.outbound.amadeus.AmadeusProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AmadeusAuthClient {

    private final WebClient amadeusWebClient;
    private final AmadeusProperties props;
    private final TokenCache cache = new TokenCache();

    public AmadeusAuthClient(WebClient amadeusWebClient, AmadeusProperties props) {
        this.amadeusWebClient = amadeusWebClient;
        this.props = props;
    }

    public String getAccessToken() {
        return cache.getValidToken().orElseGet(this::fetchAndCacheToken);
    }

    private String fetchAndCacheToken() {
        if (props.clientId() == null || props.clientId().isBlank()
                || props.clientSecret() == null || props.clientSecret().isBlank()) {
            throw new IllegalStateException("AMADEUS_CLIENT_ID / AMADEUS_CLIENT_SECRET not set");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());

        AmadeusTokenResponse token = amadeusWebClient.post()
                .uri("/v1/security/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(AmadeusTokenResponse.class)
                .block();

        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new IllegalStateException("Failed to obtain Amadeus access_token");
        }

        cache.set(token.accessToken(), token.expiresIn());
        return token.accessToken();
    }
}
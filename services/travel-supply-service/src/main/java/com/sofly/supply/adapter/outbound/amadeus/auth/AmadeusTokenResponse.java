package com.sofly.supply.adapter.outbound.amadeus.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AmadeusTokenResponse(
        String type,
        String username,
        @JsonProperty("application_name") String applicationName,
        @JsonProperty("client_id") String clientId,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn,
        String state
) {}
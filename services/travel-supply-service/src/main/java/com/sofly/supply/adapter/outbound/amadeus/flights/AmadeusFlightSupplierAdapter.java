package com.sofly.supply.adapter.outbound.amadeus.flights;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.adapter.outbound.amadeus.auth.AmadeusAuthClient;
import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AmadeusFlightSupplierAdapter implements FlightSupplierPort {

    private final WebClient amadeusWebClient;
    private final AmadeusAuthClient authClient;

    public AmadeusFlightSupplierAdapter(WebClient amadeusWebClient, AmadeusAuthClient authClient) {
        this.amadeusWebClient = amadeusWebClient;
        this.authClient = authClient;
    }

    @Override
    public JsonNode searchFlightOffers(String origin, String dest, String date, int adults, int max) {
        String token = authClient.getAccessToken();

        return amadeusWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/shopping/flight-offers")
                        .queryParam("originLocationCode", origin)
                        .queryParam("destinationLocationCode", dest)
                        .queryParam("departureDate", date)
                        .queryParam("adults", adults)
                        .queryParam("max", max)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }
}
package com.sofly.supply.adapter.outbound.amadeus.flights;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.adapter.outbound.amadeus.auth.AmadeusAuthClient;
import com.sofly.supply.application.dto.FlightSearchRequest;
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
    public String supplierKey() {
        return "amadeus";
    }

    @Override
    public JsonNode searchFlightOffers(FlightSearchRequest request) {
        String token = authClient.getAccessToken();

        return amadeusWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/shopping/flight-offers")
                        .queryParam("originLocationCode", request.getFromId())
                        .queryParam("destinationLocationCode", request.getToId())
                        .queryParam("departureDate", request.getDepartDate())
                        .queryParam("adults", request.getAdults())
                        .queryParam("max", request.getMax())
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }
}
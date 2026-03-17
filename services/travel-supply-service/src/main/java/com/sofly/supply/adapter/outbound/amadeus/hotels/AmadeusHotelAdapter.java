package com.sofly.supply.adapter.outbound.amadeus.hotels;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.adapter.outbound.amadeus.auth.AmadeusAuthClient;
import com.sofly.supply.application.port.outbound.HotelSupplierPort;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class AmadeusHotelAdapter implements HotelSupplierPort {

    private static final int MAX_HOTELS = 20;

    private final WebClient amadeusWebClient;
    private final AmadeusAuthClient authClient;

    public AmadeusHotelAdapter(WebClient amadeusWebClient, AmadeusAuthClient authClient) {
        this.amadeusWebClient = amadeusWebClient;
        this.authClient = authClient;
    }

    @Override
    public String supplierKey() {
        return "amadeus";
    }

    @Override
    public JsonNode searchHotelsByCity(String cityCode, LocalDate checkIn, LocalDate checkOut, int adults, int roomQuantity) {
        String token = authClient.getAccessToken();

        // Step 1: 도시 코드로 호텔 ID 목록 조회
        JsonNode hotelListResponse = amadeusWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reference-data/locations/hotels/by-city")
                        .queryParam("cityCode", cityCode)
                        .queryParam("radius", 5)
                        .queryParam("radiusUnit", "KM")
                        .queryParam("hotelSource", "ALL")
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (hotelListResponse == null || !hotelListResponse.has("data")) {
            throw new IllegalStateException("No hotels found for city: " + cityCode);
        }

        List<String> hotelIds = new ArrayList<>();
        for (JsonNode hotel : hotelListResponse.get("data")) {
            if (hotelIds.size() >= MAX_HOTELS) break;
            hotelIds.add(hotel.get("hotelId").asText());
        }

        if (hotelIds.isEmpty()) {
            throw new IllegalStateException("No hotel IDs found for city: " + cityCode);
        }

        // Step 2: 호텔 오퍼(가격/객실) 조회
        return amadeusWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/shopping/hotel-offers")
                        .queryParam("hotelIds", String.join(",", hotelIds))
                        .queryParam("checkInDate", checkIn)
                        .queryParam("checkOutDate", checkOut)
                        .queryParam("adults", adults)
                        .queryParam("roomQuantity", roomQuantity)
                        .queryParam("bestRateOnly", true)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }
}

package com.sofly.supply.adapter.outbound.amadeus.hotels;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.adapter.outbound.amadeus.auth.AmadeusAuthClient;
import com.sofly.supply.application.dto.HotelSearchRequest;
import com.sofly.supply.application.port.outbound.HotelSupplierPort;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.StreamSupport;

// HotelSearchRequest에 amadeus 전용 변수들 만들어줘야 됨
// amadeus 사용은 보류 상태이니 나중에 수정하도록 함

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
    public JsonNode searchHotelsByCity(HotelSearchRequest request) {
        String token = authClient.getAccessToken();

        // Step 1: 도시 코드로 호텔 ID 목록 조회
        JsonNode hotelListResponse = amadeusWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/reference-data/locations/hotels/by-city")
                        //.queryParam("cityCode", cityCode)
                        .queryParam("radius", 5)
                        .queryParam("radiusUnit", "KM")
                        .queryParam("hotelSource", "ALL")
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

//        if (hotelListResponse == null || !hotelListResponse.has("data")) {
//            throw new IllegalStateException("No hotels found for city: " + cityCode);
//        }

        List<String> hotelIds = StreamSupport
                .stream(hotelListResponse.get("data").spliterator(), false)
                .limit(MAX_HOTELS)
                .map(hotel -> hotel.get("hotelId").asText())
                .toList();

//        if (hotelIds.isEmpty()) {
//            throw new IllegalStateException("No hotel IDs found for city: " + cityCode);
//        }

        // Step 2: 호텔 오퍼(가격/객실) 조회
        return amadeusWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/shopping/hotel-offers")
                        .queryParam("hotelIds", String.join(",", hotelIds))
                        //.queryParam("checkInDate", checkIn)
                        //.queryParam("checkOutDate", checkOut)
                        //.queryParam("adults", adults)
                        //.queryParam("roomQuantity", roomQuantity)
                        .queryParam("bestRateOnly", true)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }
}

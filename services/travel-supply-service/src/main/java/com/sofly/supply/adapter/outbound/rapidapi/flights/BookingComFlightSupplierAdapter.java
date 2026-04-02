package com.sofly.supply.adapter.outbound.rapidapi.flights;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofly.supply.application.dto.FlightSearchRequest;
import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class BookingComFlightSupplierAdapter implements FlightSupplierPort {

    private final WebClient rapidApiWebClient;

    @Override
    public String supplierKey(){
        return "booking";
    }

    @Override
    public JsonNode searchFlightOffers(FlightSearchRequest request) {
        String response = rapidApiWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/api/v1/flights/searchFlights")
                            .queryParam("fromId", request.getFromId())
                            .queryParam("toId", request.getToId())
                            .queryParam("departDate", request.getDepartDate())
                            .queryParam("adults", request.getAdults());

                    if (request.getReturnDate() != null)
                        builder.queryParam("returnDate", request.getReturnDate());
                    if (request.getStops() != null)
                        builder.queryParam("stops", request.getStops().getValue());
                    if (request.getCabinClass() != null)
                        builder.queryParam("cabinClass", request.getCabinClass());
                    if (request.getCurrencyCode() != null)
                        builder.queryParam("currency_code", request.getCurrencyCode());

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseJson(response);
    }

    private JsonNode parseJson(String response) {
        try {
            return new ObjectMapper().readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Booking.com 응답 파싱 실패", e);
        }
    }
}

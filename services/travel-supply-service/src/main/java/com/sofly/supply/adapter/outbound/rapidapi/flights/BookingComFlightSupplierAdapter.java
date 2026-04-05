package com.sofly.supply.adapter.outbound.rapidapi.flights;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.adapter.outbound.rapidapi.RapidApiJsonUtils;
import com.sofly.supply.application.dto.FlightSearchRequest;
import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
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
                    if (request.getSort() != null)
                        builder.queryParam("sort", request.getSort());
                    if (request.getCabinClass() != null)
                        builder.queryParam("cabinClass", request.getCabinClass());
                    if (request.getCurrencyCode() != null)
                        builder.queryParam("currency_code", request.getCurrencyCode());
                    if (request.getChildrenAge() != null && !request.getChildrenAge().isBlank())
                        builder.queryParam("children", request.getChildrenAge());
                    if (request.getPageNo() != null)
                        builder.queryParam("pageNo", request.getPageNo());

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null) return RapidApiJsonUtils.nullNode();
        return RapidApiJsonUtils.parseJson(response);
    }
}

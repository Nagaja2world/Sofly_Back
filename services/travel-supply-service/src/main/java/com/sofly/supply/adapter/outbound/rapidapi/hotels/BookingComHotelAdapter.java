package com.sofly.supply.adapter.outbound.rapidapi.hotels;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofly.supply.application.dto.HotelSearchRequest;
import com.sofly.supply.application.port.outbound.HotelSupplierPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class BookingComHotelAdapter implements  HotelSupplierPort {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient rapidApiWebClient;

    @Override
    public String supplierKey(){
        return "booking";
    }

    @Override
    public JsonNode searchHotelsByCity(HotelSearchRequest request){

        String response = rapidApiWebClient.get()
                .uri(urlBuilder ->{
                        var builder = urlBuilder
                                .path("/api/v1/hotels/searchHotels")
                                .queryParam("destId", request.getDestId())
                                .queryParam("searchType", request.getSearchType())
                                .queryParam("arrivalDate", request.getArrivalDate())
                                .queryParam("departureDate", request.getDepartureDate());

                                if (request.getAdults() != null)
                                    builder.queryParam("adults", request.getAdults());
                                if (request.getChildrenAge()!= null)
                                    builder.queryParam("childrenAge", request.getChildrenAge());
                                if (request.getRoomQty() != null)
                                    builder.queryParam("roomQty", request.getRoomQty());
                                if (request.getPriceMin() != null)
                                    builder.queryParam("priceMin", request.getPriceMin());
                                if (request.getPriceMax() != null)
                                    builder.queryParam("priceMax", request.getPriceMax());
                                if (request.getSortBy() != null)
                                    builder.queryParam("sortBy", request.getSortBy());
                                if (request.getCategoriesFilter() != null)
                                    builder.queryParam("categoriesFilter", request.getCategoriesFilter());
                                if (request.getPageNumber() != null)
                                    builder.queryParam("pageNumber", request.getPageNumber());
                                if (request.getUnits() != null)
                                    builder.queryParam("units", request.getUnits());
                                if (request.getTemperatureUnit() != null)
                                    builder.queryParam("temperatureUnit", request.getTemperatureUnit());
                                if (request.getLanguageCode() != null)
                                    builder.queryParam("languageCode", request.getLanguageCode());
                                if (request.getCurrencyCode() != null)
                                    builder.queryParam("currencyCode", request.getCurrencyCode());
                                if (request.getLocation() != null)
                                    builder.queryParam("location", request.getLocation());

                                var uri = builder.build();
                                return uri;
                })
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseJson(response);
    }

    private JsonNode parseJson(String response) {
        try {
            return OBJECT_MAPPER.readTree(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Booking.com 응답 파싱 실패", e);
        }
    }
}

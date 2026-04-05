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
                .uri(urlBuilder -> {
                        var builder = urlBuilder
                                .path("/api/v1/hotels/searchHotels")
                                .queryParam("dest_id", request.getDestId())
                                .queryParam("search_type", request.getSearchType())
                                .queryParam("arrival_date", request.getArrivalDate())
                                .queryParam("departure_date", request.getDepartureDate());

                                if (request.getAdults() != null)
                                    builder.queryParam("adults", request.getAdults());
                                if (request.getChildrenAge() != null)
                                    builder.queryParam("children_age", request.getChildrenAge());
                                if (request.getRoomQty() != null)
                                    builder.queryParam("room_qty", request.getRoomQty());
                                if (request.getPriceMin() != null)
                                    builder.queryParam("price_min", request.getPriceMin());
                                if (request.getPriceMax() != null)
                                    builder.queryParam("price_max", request.getPriceMax());
                                if (request.getSortBy() != null)
                                    builder.queryParam("sort_by", request.getSortBy());
                                if (request.getCategoriesFilter() != null)
                                    builder.queryParam("categories_filter", request.getCategoriesFilter());
                                if (request.getPageNumber() != null)
                                    builder.queryParam("page_number", request.getPageNumber());
                                if (request.getUnits() != null)
                                    builder.queryParam("units", request.getUnits().name().toLowerCase());
                                if (request.getTemperatureUnit() != null)
                                    builder.queryParam("temperature_unit", request.getTemperatureUnit().getValue());
                                if (request.getLanguageCode() != null)
                                    builder.queryParam("languagecode", request.getLanguageCode());
                                if (request.getCurrencyCode() != null)
                                    builder.queryParam("currency_code", request.getCurrencyCode());
                                if (request.getLocation() != null)
                                    builder.queryParam("location", request.getLocation());

                                return builder.build();
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

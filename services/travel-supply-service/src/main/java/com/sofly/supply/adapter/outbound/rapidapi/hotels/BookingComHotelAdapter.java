package com.sofly.supply.adapter.outbound.rapidapi.hotels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sofly.supply.adapter.outbound.rapidapi.BookingDeepLinkBuilder;
import com.sofly.supply.adapter.outbound.rapidapi.RapidApiJsonUtils;
import com.sofly.supply.application.dto.HotelDetailsRequest;
import com.sofly.supply.application.dto.HotelSearchRequest;
import com.sofly.supply.application.port.outbound.HotelSupplierPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class BookingComHotelAdapter implements HotelSupplierPort {

    private final WebClient rapidApiWebClient;

    @Override
    public String supplierKey(){
        return "booking";
    }

    @Cacheable(value = "bookingHotels", key = "#request")
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

        if (response == null) return RapidApiJsonUtils.nullNode();

        JsonNode root = RapidApiJsonUtils.parseJson(response);
        injectHotelDeepLinks(root, request);
        return root;
    }

    @Cacheable(
            value = "bookingHotelDetails",
            key = "#request",
            unless = "#result == null || #result.path('status').asBoolean() == false || #result.path('data').isMissingNode()"
    )
    @Override
    public JsonNode getHotelDetails(HotelDetailsRequest request) {
        String response = rapidApiWebClient.get()
                .uri(urlBuilder -> {
                    var builder = urlBuilder
                            .path("/api/v1/hotels/getHotelDetails")
                            .queryParam("hotel_id", request.getHotelId())
                            .queryParam("arrival_date", request.getArrivalDate())
                            .queryParam("departure_date", request.getDepartureDate());

                    if (request.getAdults() != null)
                        builder.queryParam("adults", request.getAdults());
                    if (request.getChildrenAge() != null)
                        builder.queryParam("children_age", request.getChildrenAge());
                    if (request.getRoomQty() != null)
                        builder.queryParam("room_qty", request.getRoomQty());
                    if (request.getUnits() != null)
                        builder.queryParam("units", request.getUnits().name().toLowerCase());
                    if (request.getTemperatureUnit() != null)
                        builder.queryParam("temperature_unit", request.getTemperatureUnit().getValue());
                    if (request.getLanguageCode() != null)
                        builder.queryParam("languagecode", request.getLanguageCode());
                    if (request.getCurrencyCode() != null)
                        builder.queryParam("currency_code", request.getCurrencyCode());

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null) return RapidApiJsonUtils.nullNode();
        return RapidApiJsonUtils.parseJson(response);
    }

    /**
     * 각 호텔의 property 노드에 bookingUrl 필드를 추가한다.
     * 호텔 slug는 API가 제공하지 않으므로 property.name을 slugify하여 사용.
     */
    private void injectHotelDeepLinks(JsonNode root, HotelSearchRequest request) {
        JsonNode hotels = root.path("data").path("hotels");
        if (!hotels.isArray()) return;

        int adults = request.getAdults() != null ? request.getAdults() : 1;
        String checkin = request.getArrivalDate() != null ? request.getArrivalDate().toString() : null;
        String checkout = request.getDepartureDate() != null ? request.getDepartureDate().toString() : null;

        for (JsonNode hotel : hotels) {
            JsonNode property = hotel.path("property");
            if (!property.isObject()) continue;

            String name = property.path("name").asText(null);
            String countryCode = property.path("countryCode").asText(null);

            if (name != null && countryCode != null && checkin != null && checkout != null) {
                String bookingUrl = BookingDeepLinkBuilder.buildHotelUrl(
                        name, countryCode, checkin, checkout, adults);
                ((ObjectNode) property).put("bookingUrl", bookingUrl);
            }
        }
    }
}

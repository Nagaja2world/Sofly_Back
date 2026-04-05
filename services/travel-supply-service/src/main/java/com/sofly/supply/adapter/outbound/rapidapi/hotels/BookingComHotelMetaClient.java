package com.sofly.supply.adapter.outbound.rapidapi.hotels;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.dto.HotelDestination;
import com.sofly.supply.application.dto.HotelOptionsRequest;
import com.sofly.supply.application.dto.HotelSortOption;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class BookingComHotelMetaClient {

    private static final Logger log = LoggerFactory.getLogger(BookingComHotelMetaClient.class);

    private final WebClient rapidApiWebClient;

    public List<HotelDestination> searchDestination(String query) {
        try {
            JsonNode response = rapidApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/hotels/searchDestination")
                            .queryParam("query", query)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<HotelDestination> results = new ArrayList<>();

            if (response == null || !response.has("data")) {
                return results;
            }

            for (JsonNode item : response.get("data")) {
                results.add(new HotelDestination(
                        textOrNull(item, "dest_id"),
                        textOrNull(item, "dest_type"),
                        textOrNull(item, "name"),
                        textOrNull(item, "label"),
                        textOrNull(item, "city_name"),
                        textOrNull(item, "country"),
                        textOrNull(item, "region"),
                        doubleOrNull(item, "latitude"),
                        doubleOrNull(item, "longitude"),
                        textOrNull(item, "image_url"),
                        intOrNull(item, "hotels")
                ));
            }

            return results;

        } catch (WebClientResponseException e) {
            log.warn("Booking.com searchDestination API error: {} {}", e.getStatusCode(), e.getMessage());
            return List.of();
        }
    }

    public List<HotelSortOption> getSortBy(HotelOptionsRequest request) {
        try {
            JsonNode response = rapidApiWebClient.get()
                    .uri(optionsUriBuilder("/api/v1/hotels/getSortBy", request, false))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<HotelSortOption> results = new ArrayList<>();

            if (response == null || !response.has("data")) {
                return results;
            }

            for (JsonNode item : response.get("data")) {
                results.add(new HotelSortOption(
                        textOrNull(item, "id"),
                        textOrNull(item, "title")
                ));
            }

            return results;

        } catch (WebClientResponseException e) {
            log.warn("Booking.com getSortBy API error: {} {}", e.getStatusCode(), e.getMessage());
            return List.of();
        }
    }

    public JsonNode getFilter(HotelOptionsRequest request) {
        try {
            JsonNode response = rapidApiWebClient.get()
                    .uri(optionsUriBuilder("/api/v1/hotels/getFilter", request, true))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("data")) {
                return null;
            }

            JsonNode data = response.get("data");
            return data.has("filters") ? data.get("filters") : data;

        } catch (WebClientResponseException e) {
            log.warn("Booking.com getFilter API error: {} {}", e.getStatusCode(), e.getMessage());
            return null;
        }
    }

    private Function<UriBuilder, URI> optionsUriBuilder(String path, HotelOptionsRequest req, boolean includeCategories) {
        return uriBuilder -> {
            var builder = uriBuilder
                    .path(path)
                    .queryParam("dest_id", req.getDestId())
                    .queryParam("search_type", req.getSearchType())
                    .queryParam("arrival_date", req.getArrivalDate())
                    .queryParam("departure_date", req.getDepartureDate());

            if (req.getAdults() != null)
                builder.queryParam("adults", req.getAdults());
            if (req.getChildrenAge() != null)
                builder.queryParam("children_age", req.getChildrenAge());
            if (req.getRoomQty() != null)
                builder.queryParam("room_qty", req.getRoomQty());
            if (includeCategories && req.getCategoriesFilter() != null)
                builder.queryParam("categories_filter", req.getCategoriesFilter());

            return builder.build();
        };
    }

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private Double doubleOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asDouble() : null;
    }

    private Integer intOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : null;
    }
}

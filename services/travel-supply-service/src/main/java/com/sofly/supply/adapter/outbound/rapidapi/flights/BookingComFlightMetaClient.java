package com.sofly.supply.adapter.outbound.rapidapi.flights;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.dto.FlightDestination;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingComFlightMetaClient {
    private static final Logger log = LoggerFactory.getLogger(BookingComFlightMetaClient.class);

    private final WebClient rapidApiWebClient;

    public List<FlightDestination> searchDestinations(String query, String languageCode){
        try{
            JsonNode response = rapidApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/flights/searchDestination")
                            .queryParam("query", query)
                            .queryParam("languagecode", languageCode)
                            .build()
                    )
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<FlightDestination> results = new ArrayList<>();

            if (response == null || !response.has("data")) {
                return results;
            }

            for (JsonNode item : response.get("data")) {
                FlightDestination.DistanceToCity distanceToCity = null;
                if (item.has("distanceToCity") && !item.get("distanceToCity").isNull()) {
                    JsonNode d = item.get("distanceToCity");
                    distanceToCity = new FlightDestination.DistanceToCity(
                            d.get("value").asDouble(),
                            d.get("unit").asText()
                    );
                }

                results.add(new FlightDestination(
                        textOrNull(item, "id"),
                        textOrNull(item, "type"),
                        textOrNull(item, "name"),
                        textOrNull(item, "code"),
                        textOrNull(item, "city"),
                        textOrNull(item, "cityName"),
                        textOrNull(item, "regionName"),
                        textOrNull(item, "country"),
                        textOrNull(item, "countryName"),
                        textOrNull(item, "countryNameShort"),
                        textOrNull(item, "photoUri"),
                        distanceToCity,
                        textOrNull(item, "parent")
                ));
            }

            return results;
        } catch (WebClientResponseException e){
            log.warn("Booking.com searchDestination API error: {} {}", e.getStatusCode(), e.getMessage());
            return List.of();
        }
    }

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }
}

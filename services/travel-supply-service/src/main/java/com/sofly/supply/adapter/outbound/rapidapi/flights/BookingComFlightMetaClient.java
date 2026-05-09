package com.sofly.supply.adapter.outbound.rapidapi.flights;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.adapter.outbound.rapidapi.RapidApiJsonUtils;
import com.sofly.supply.application.dto.FlightDestination;
import com.sofly.supply.application.port.outbound.FlightMetaPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingComFlightMetaClient implements FlightMetaPort {
    private static final Logger log = LoggerFactory.getLogger(BookingComFlightMetaClient.class);

    private final WebClient rapidApiWebClient;

    @Cacheable(
        value = "flightDestinations",
        key = "#query.toLowerCase().trim() + ':' + #languageCode",
        condition = "#query != null && #query.trim().length() >= 2",
        unless = "#result == null || #result.isEmpty()" 
    )
    public List<FlightDestination> searchDestinations(String query, String languageCode){

        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
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
                if (item.hasNonNull("distanceToCity")) {
                    JsonNode d = item.get("distanceToCity");
                    distanceToCity = new FlightDestination.DistanceToCity(
                            d.get("value").asDouble(),
                            d.get("unit").asText()
                    );
                }

                results.add(new FlightDestination(
                        RapidApiJsonUtils.textOrNull(item, "id"),
                        RapidApiJsonUtils.textOrNull(item, "type"),
                        RapidApiJsonUtils.textOrNull(item, "name"),
                        RapidApiJsonUtils.textOrNull(item, "code"),
                        RapidApiJsonUtils.textOrNull(item, "city"),
                        RapidApiJsonUtils.textOrNull(item, "cityName"),
                        RapidApiJsonUtils.textOrNull(item, "regionName"),
                        RapidApiJsonUtils.textOrNull(item, "country"),
                        RapidApiJsonUtils.textOrNull(item, "countryName"),
                        RapidApiJsonUtils.textOrNull(item, "countryNameShort"),
                        RapidApiJsonUtils.textOrNull(item, "photoUri"),
                        distanceToCity,
                        RapidApiJsonUtils.textOrNull(item, "parent")
                ));
            }

            return results;
        } catch (WebClientResponseException e){
            log.warn("Booking.com searchDestination API error: {} {}", e.getStatusCode(), e.getMessage());
            return List.of();
        }
    }

}

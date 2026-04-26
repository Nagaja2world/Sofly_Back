package com.sofly.core.global.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.core.global.client.dto.*;
import com.sofly.core.global.client.dto.PlacesResponse.PhotoMedia;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "supply-service", url = "${sofly.supply.url}")
public interface SupplyClient {

    // ── 항공권 ──────────────────────────────────────────────────────────

    @GetMapping("/supply/flights/offers")
    JsonNode searchFlights(
            @RequestParam(required = false) String supplier,
            @SpringQueryMap FlightSearchRequest request
    );

    @GetMapping("/supply/flights/destinations")
    List<FlightDestination> searchFlightDestinations(
            @RequestParam String query,
            @RequestParam String languageCode
    );

    @GetMapping("/supply/flights/details")
    JsonNode getFlightDetails(
            @RequestParam(required = false) String supplier,
            @RequestParam String token,
            @RequestParam(required = false, defaultValue = "KRW") String currencyCode
    );

    // ── 호텔 ────────────────────────────────────────────────────────────

    @GetMapping("/supply/hotels/offers")
    JsonNode searchHotels(
            @RequestParam(required = false) String supplier,
            @SpringQueryMap HotelSearchRequest request
    );

    @GetMapping("/supply/hotels/destinations")
    List<HotelDestination> searchHotelDestinations(@RequestParam String query);

    @GetMapping("/supply/hotels/sort-options")
    List<HotelSortOption> getHotelSortOptions(@SpringQueryMap HotelOptionsRequest request);

    @GetMapping("/supply/hotels/filter-options")
    JsonNode getHotelFilterOptions(@SpringQueryMap HotelOptionsRequest request);

    // ── 장소 ────────────────────────────────────────────────────────────

    @GetMapping("/supply/places")
    PlacesResponse searchPlace(@RequestParam String text);

    @GetMapping("/supply/places/photo")
    PhotoMedia getPlacePhoto(
            @RequestParam String name,
            @RequestParam(defaultValue = "800") int maxWidthPx
    );
}

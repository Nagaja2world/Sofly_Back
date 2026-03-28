package com.sofly.supply.adapter.inbound.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.service.FlightSearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supply/flights")
public class FlightSearchController {

    private final FlightSearchService flightSearchService;

    public FlightSearchController(FlightSearchService flightSearchService) {
        this.flightSearchService = flightSearchService;
    }

    @GetMapping("/offers")
    public JsonNode offers(
            @RequestParam(required = false) String supplier,  // 예: amadeus, skyscanner
            @RequestParam String origin,
            @RequestParam String dest,
            @RequestParam String date,
            @RequestParam(defaultValue = "1") int adults,
            @RequestParam(defaultValue = "5") int max
    ) {
        return flightSearchService.search(supplier, origin, dest, date, adults, max);

    }
}

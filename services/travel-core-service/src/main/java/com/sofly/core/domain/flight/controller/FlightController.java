package com.sofly.core.domain.flight.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.core.domain.flight.service.FlightService;
import com.sofly.core.global.client.dto.FlightDestination;
import com.sofly.core.global.client.dto.FlightSearchRequest;
import com.sofly.core.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;

@Tag(name = "Flight", description = "항공권 검색")
@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @Operation(summary = "항공권 검색")
    @GetMapping("/offers")
    public ResponseEntity<ApiResponse<JsonNode>> searchFlights(
            @RequestParam(required = false) String supplier,
            @ParameterObject FlightSearchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(flightService.searchFlights(supplier, request)));
    }

    @Operation(summary = "항공권 목적지 자동완성")
    @GetMapping("/destinations")
    public ResponseEntity<ApiResponse<List<FlightDestination>>> searchFlightDestinations(
            @RequestParam String query,
            @RequestParam String languageCode
    ) {
        return ResponseEntity.ok(ApiResponse.success(flightService.searchFlightDestinations(query, languageCode)));
    }
}

package com.sofly.core.domain.flight.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.core.domain.flight.service.FlightService;
import com.sofly.core.global.client.dto.FlightDestination;
import com.sofly.core.global.client.dto.FlightSearchRequest;
import com.sofly.core.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;

@Tag(name = "Flight", description = "항공권 검색 API")
@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @Operation(
            summary = "항공편 검색",
            description = "공급자(supplier)를 선택하여 항공편 오퍼를 검색합니다. 기본값: booking"
    )
    @GetMapping("/offers")
    public ResponseEntity<ApiResponse<JsonNode>> searchFlights(
            @Parameter(description = "공급자 키 (booking)", example = "booking")
            @RequestParam(required = false) String supplier,
            @ParameterObject FlightSearchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(flightService.searchFlights(supplier, request)));
    }

    @Operation(
            summary = "공항 검색",
            description = "항공 검색에 필요한 공항 id를 조회합니다. (fromId / toId 용)"
    )
    @GetMapping("/destinations")
    public ResponseEntity<ApiResponse<List<FlightDestination>>> searchFlightDestinations(
            @Parameter(description = "검색어 (공항명, 도시명, 국가명 등)", example = "korea")
            @RequestParam String query,
            @Parameter(description = "언어 코드", example = "en-us")
            @RequestParam String languageCode
    ) {
        return ResponseEntity.ok(ApiResponse.success(flightService.searchFlightDestinations(query, languageCode)));
    }

    @Operation(
            summary = "항공편 상세 조회",
            description = "항공편 검색(GET /offers) 응답에 포함된 token으로 특정 항공편의 상세 정보를 조회합니다."
    )
    @GetMapping("/details")
    public ResponseEntity<ApiResponse<JsonNode>> getFlightDetails(
            @Parameter(description = "공급자 키 (booking)", example = "booking")
            @RequestParam(required = false) String supplier,
            @Parameter(description = "항공편 token — /offers 응답의 각 항공편 token 필드 값", required = true)
            @RequestParam String token,
            @Parameter(description = "통화 코드", example = "KRW")
            @RequestParam(required = false, defaultValue = "KRW") String currencyCode
    ) {
        return ResponseEntity.ok(ApiResponse.success(flightService.getFlightDetails(supplier, token, currencyCode)));
    }
}

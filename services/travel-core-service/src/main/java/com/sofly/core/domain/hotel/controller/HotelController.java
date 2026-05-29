package com.sofly.core.domain.hotel.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.core.domain.hotel.service.HotelService;
import com.sofly.core.global.client.dto.HotelDestination;
import com.sofly.core.global.client.dto.HotelDetailsRequest;
import com.sofly.core.global.client.dto.HotelOptionsRequest;
import com.sofly.core.global.client.dto.HotelSearchRequest;
import com.sofly.core.global.client.dto.HotelSortOption;
import com.sofly.core.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

import java.beans.PropertyEditorSupport;
import java.util.List;

@Tag(name = "Hotel", description = "호텔 검색")
@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(HotelSearchRequest.Units.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(text == null || text.isBlank() ? null : HotelSearchRequest.Units.valueOf(text.toUpperCase()));
            }
        });
        binder.registerCustomEditor(HotelSearchRequest.TemperatureUnit.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.isBlank()) {
                    setValue(null);
                    return;
                }
                setValue("c".equalsIgnoreCase(text)
                        ? HotelSearchRequest.TemperatureUnit.CELSIUS
                        : "f".equalsIgnoreCase(text)
                        ? HotelSearchRequest.TemperatureUnit.FAHRENHEIT
                        : HotelSearchRequest.TemperatureUnit.valueOf(text.toUpperCase()));
            }
        });
    }

    @Operation(summary = "호텔 검색")
    @GetMapping("/offers")
    public ResponseEntity<ApiResponse<JsonNode>> searchHotels(
            @RequestParam(required = false) String supplier,
            @ParameterObject HotelSearchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hotelService.searchHotels(supplier, request)));
    }

    @Operation(summary = "호텔 상세 조회")
    @GetMapping("/details")
    public ResponseEntity<ApiResponse<JsonNode>> getHotelDetails(
            @RequestParam(required = false) String supplier,
            @ParameterObject HotelDetailsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hotelService.getHotelDetails(supplier, request)));
    }

    @Operation(summary = "호텔 목적지 자동완성")
    @GetMapping("/destinations")
    public ResponseEntity<ApiResponse<List<HotelDestination>>> searchHotelDestinations(
            @RequestParam String query
    ) {
        return ResponseEntity.ok(ApiResponse.success(hotelService.searchHotelDestinations(query)));
    }

    @Operation(summary = "호텔 정렬 옵션 조회")
    @GetMapping("/sort-options")
    public ResponseEntity<ApiResponse<List<HotelSortOption>>> getHotelSortOptions(
            @ParameterObject HotelOptionsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hotelService.getHotelSortOptions(request)));
    }

    @Operation(summary = "호텔 필터 옵션 조회")
    @GetMapping("/filter-options")
    public ResponseEntity<ApiResponse<JsonNode>> getHotelFilterOptions(
            @ParameterObject HotelOptionsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(hotelService.getHotelFilterOptions(request)));
    }
}

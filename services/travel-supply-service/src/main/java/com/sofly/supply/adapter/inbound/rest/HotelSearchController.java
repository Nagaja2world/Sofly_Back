package com.sofly.supply.adapter.inbound.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.adapter.outbound.google.PlaceInfo;
import com.sofly.supply.application.dto.HotelDestination;
import com.sofly.supply.application.dto.HotelOptionsRequest;
import com.sofly.supply.application.dto.HotelSearchRequest;
import com.sofly.supply.application.dto.HotelSortOption;
import com.sofly.supply.application.service.HotelSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;

@Tag(name = "Hotel Search", description = "호텔 검색 API")
@RestController
@RequestMapping("/supply/hotels")
public class HotelSearchController {

    private final HotelSearchService hotelSearchService;

    public HotelSearchController(HotelSearchService hotelSearchService) {
        this.hotelSearchService = hotelSearchService;
    }

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
                setValue(text == null || text.isBlank() ? null : HotelSearchRequest.TemperatureUnit.valueOf(text.toUpperCase()));
            }
        });
    }

    @Operation(summary = "호텔 검색", description = "공급자(supplier)를 선택하여 호텔 오퍼를 검색합니다. 기본값: booking")
    @GetMapping("/offers")
    public JsonNode offers(
            @Parameter(description = "공급자 키 (booking | amadeus)", example = "booking")
            @RequestParam(required = false) String supplier,
            @ParameterObject @ModelAttribute HotelSearchRequest request
    ) {
        return hotelSearchService.search(supplier, request);
    }

    @Operation(summary = "호텔 목적지 검색", description = "도시/지역명으로 dest_id와 search_type을 조회합니다. 호텔 검색 전에 먼저 호출하세요. 반환값에서 dest_id와 dest_type을 사용하면 됩니다.")
    @GetMapping("/destinations")
    public List<HotelDestination> searchDestination(
            @Parameter(description = "검색어 (도시명, 지역명 등)", example = "seoul")
            @RequestParam String query
    ) {
        return hotelSearchService.searchDestination(query);
    }

    @Operation(summary = "정렬 옵션 조회", description = "호텔 검색에 사용 가능한 sort_by 값 목록을 조회합니다.")
    @GetMapping("/sort-options")
    public List<HotelSortOption> getSortBy(@ParameterObject @ModelAttribute HotelOptionsRequest request) {
        return hotelSearchService.getSortBy(request);
    }

    @Operation(summary = "필터 옵션 조회", description = "호텔 검색에 사용 가능한 categories_filter 값 목록을 조회합니다. 초기 요청 시 categoriesFilter는 비워두세요.")
    @GetMapping("/filter-options")
    public JsonNode getFilter(@ParameterObject @ModelAttribute HotelOptionsRequest request) {
        return hotelSearchService.getFilter(request);
    }

    @GetMapping("/place-info")
    public PlaceInfo placeInfo(
            @RequestParam String hotelName,
            @RequestParam String cityCode
    ) {
        return hotelSearchService.getPlaceInfo(hotelName, cityCode);
    }
}

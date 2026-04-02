package com.sofly.supply.adapter.inbound.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.dto.FlightSearchRequest;
import com.sofly.supply.application.service.FlightSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;

@Tag(name = "Flight Search", description = "항공편 검색 API")
@RestController
@RequestMapping("/supply/flights")
public class FlightSearchController {

    private final FlightSearchService flightSearchService;

    public FlightSearchController(FlightSearchService flightSearchService) {
        this.flightSearchService = flightSearchService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(FlightSearchRequest.StopsType.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(text == null || text.isBlank() ? null : FlightSearchRequest.StopsType.valueOf(text));
            }
        });
        binder.registerCustomEditor(FlightSearchRequest.SortType.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(text == null || text.isBlank() ? null : FlightSearchRequest.SortType.valueOf(text));
            }
        });
        binder.registerCustomEditor(FlightSearchRequest.CabinClass.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(text == null || text.isBlank() ? null : FlightSearchRequest.CabinClass.valueOf(text));
            }
        });
    }

    @Operation(summary = "항공편 검색", description = "공급자(supplier)를 선택하여 항공편 오퍼를 검색합니다. 기본값: amadeus")
    @GetMapping("/offers")
    public JsonNode offers(
            @Parameter(description = "공급자 키 (amadeus | booking)", example = "amadeus")
            @RequestParam(required = false) String supplier,
            @ParameterObject @ModelAttribute FlightSearchRequest request
    ) {
        return flightSearchService.search(supplier, request);
    }
}

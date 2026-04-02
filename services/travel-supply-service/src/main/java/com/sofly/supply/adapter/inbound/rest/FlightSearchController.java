package com.sofly.supply.adapter.inbound.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.dto.FlightSearchRequest;
import com.sofly.supply.application.service.FlightSearchService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;

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

    @GetMapping("/offers")
    public JsonNode offers(
            @RequestParam(required = false) String supplier,  // 예: amadeus, booking
            @ParameterObject @ModelAttribute FlightSearchRequest request
            ) {
        return flightSearchService.search(supplier, request);

    }
}

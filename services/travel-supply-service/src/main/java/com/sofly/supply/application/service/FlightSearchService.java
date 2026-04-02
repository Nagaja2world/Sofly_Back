package com.sofly.supply.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.dto.FlightSearchRequest;
import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import org.springframework.stereotype.Service;

@Service
public class FlightSearchService {

    private final SupplierRouter router;

    public FlightSearchService(SupplierRouter router) {
        this.router = router;
    }

    public JsonNode search(String supplier, FlightSearchRequest request) {
        FlightSupplierPort selected = router.selectFlightSupplier(supplier);
        return selected.searchFlightOffers(request);
    }
}
package com.sofly.supply.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import org.springframework.stereotype.Service;

@Service
public class FlightSearchService {

    private final SupplierRouter router;

    public FlightSearchService(SupplierRouter router) {
        this.router = router;
    }

    public JsonNode search(String supplier, String origin, String dest, String date, int adults, int max) {
        FlightSupplierPort selected = router.selectFlightSupplier(supplier);
        return selected.searchFlightOffers(origin, dest, date, adults, max);
    }
}
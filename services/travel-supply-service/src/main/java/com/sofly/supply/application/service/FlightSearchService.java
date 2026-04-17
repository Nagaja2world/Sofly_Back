package com.sofly.supply.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.dto.FlightDestination;
import com.sofly.supply.application.dto.FlightSearchRequest;
import com.sofly.supply.application.port.outbound.FlightMetaPort;
import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightSearchService {

    private final SupplierRouter router;
    private final FlightMetaPort flightMetaPort;

    public FlightSearchService(SupplierRouter router, FlightMetaPort flightMetaPort) {
        this.router = router;
        this.flightMetaPort = flightMetaPort;
    }

    public JsonNode search(String supplier, FlightSearchRequest request) {
        FlightSupplierPort selected = router.selectFlightSupplier(supplier);
        return selected.searchFlightOffers(request);
    }

    public List<FlightDestination> searchDestination(String query, String languageCode) {
        return flightMetaPort.searchDestinations(query, languageCode);
    }

    public JsonNode getFlightDetails(String supplier, String token, String currencyCode) {
        FlightSupplierPort selected = router.selectFlightSupplier(supplier);
        return selected.getFlightDetails(token, currencyCode);
    }
}
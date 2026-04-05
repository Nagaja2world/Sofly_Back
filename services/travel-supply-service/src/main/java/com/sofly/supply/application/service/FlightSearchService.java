package com.sofly.supply.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.adapter.outbound.rapidapi.flights.BookingComFlightMetaClient;
import com.sofly.supply.application.dto.FlightDestination;
import com.sofly.supply.application.dto.FlightSearchRequest;
import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightSearchService {

    private final SupplierRouter router;
    private final BookingComFlightMetaClient bookingComFlightMetaClient;

    public FlightSearchService(SupplierRouter router, BookingComFlightMetaClient bookingComFlightMetaClient) {
        this.router = router;
        this.bookingComFlightMetaClient = bookingComFlightMetaClient;
    }

    public JsonNode search(String supplier, FlightSearchRequest request) {
        FlightSupplierPort selected = router.selectFlightSupplier(supplier);
        return selected.searchFlightOffers(request);
    }

    public List<FlightDestination> searchDestination(String query, String languageCode) {
        return bookingComFlightMetaClient.searchDestinations(query, languageCode);
    }
}
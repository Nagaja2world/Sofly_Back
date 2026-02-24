package com.sofly.supply.application.port.outbound;

import com.fasterxml.jackson.databind.JsonNode;

public interface FlightSupplierPort {
    JsonNode searchFlightOffers(String origin, String dest, String date, int adults, int max);
}
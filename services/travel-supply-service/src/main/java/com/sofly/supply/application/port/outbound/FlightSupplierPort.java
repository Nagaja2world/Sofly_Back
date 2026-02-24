package com.sofly.supply.application.port.outbound;

import com.fasterxml.jackson.databind.JsonNode;

public interface FlightSupplierPort {

    /*"amadeus" or 다른 외부 api */
    String supplierKey();

    JsonNode searchFlightOffers(String origin, String dest, String date, int adults, int max);
}
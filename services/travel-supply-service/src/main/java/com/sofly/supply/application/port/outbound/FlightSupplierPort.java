package com.sofly.supply.application.port.outbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.dto.FlightSearchRequest;

public interface FlightSupplierPort {

    /*"amadeus" or 다른 외부 api */
    String supplierKey();

    JsonNode searchFlightOffers(FlightSearchRequest request);
}
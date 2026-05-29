package com.sofly.supply.application.port.outbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.dto.HotelDetailsRequest;
import com.sofly.supply.application.dto.HotelSearchRequest;

public interface HotelSupplierPort {

    /* "amadeus" or 다른 외부 api */
    String supplierKey();

    JsonNode searchHotelsByCity(HotelSearchRequest request);

    JsonNode getHotelDetails(HotelDetailsRequest request);
}

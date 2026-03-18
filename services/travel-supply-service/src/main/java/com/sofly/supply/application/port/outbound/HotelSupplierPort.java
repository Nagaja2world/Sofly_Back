package com.sofly.supply.application.port.outbound;

import com.fasterxml.jackson.databind.JsonNode;

public interface HotelSupplierPort {

    /* "amadeus" or 다른 외부 api */
    String supplierKey();

    JsonNode searchHotelsByCity(String cityCode, java.time.LocalDate checkIn, java.time.LocalDate checkOut, int adults, int roomQuantity);
}

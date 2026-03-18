package com.sofly.supply.application.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record HotelSearchResult(
        String hotelId,
        String hotelName,
        String cityCode,
        Double latitude,
        Double longitude,
        JsonNode offers        // Amadeus 오퍼 (가격, 객실 정보 등)
) {}

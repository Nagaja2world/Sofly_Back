package com.sofly.supply.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.adapter.outbound.google.PlaceInfo;

public record HotelSearchResult(
        String hotelId,
        String hotelName,
        String cityCode,
        Double latitude,
        Double longitude,
        JsonNode offers,       // Amadeus 오퍼 (가격, 객실 정보 등)
        PlaceInfo placeInfo    // Google Places 보완 정보 (평점, 리뷰 수, 사진)
) {}

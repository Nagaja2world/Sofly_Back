package com.sofly.supply.application.dto;

public record PlaceInfo(
        String placeId,
        Double rating,
        Integer userRatingsTotal
) {}

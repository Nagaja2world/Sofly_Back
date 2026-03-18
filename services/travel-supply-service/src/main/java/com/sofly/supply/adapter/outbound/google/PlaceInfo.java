package com.sofly.supply.adapter.outbound.google;

import java.util.List;

public record PlaceInfo(
        String placeId,
        Double rating,
        Integer userRatingsTotal
        //List<String> photoUrls
) {}

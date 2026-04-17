package com.sofly.core.global.client.dto;

public record HotelDestination(
        String destId,
        String destType,
        String name,
        String label,
        String cityName,
        String country,
        String region,
        Double latitude,
        Double longitude,
        String imageUrl,
        Integer hotels
) {}

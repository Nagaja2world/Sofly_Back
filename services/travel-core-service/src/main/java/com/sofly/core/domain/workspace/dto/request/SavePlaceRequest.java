package com.sofly.core.domain.workspace.dto.request;


import jakarta.validation.constraints.NotBlank;

public record SavePlaceRequest(
        @NotBlank
        String placeId,
        @NotBlank
        String name,
        String address,
        Double latitude,
        Double longitude,
        String primaryType,
        String photoReference,
        Double rating,
        String googleMapsUri
) {}

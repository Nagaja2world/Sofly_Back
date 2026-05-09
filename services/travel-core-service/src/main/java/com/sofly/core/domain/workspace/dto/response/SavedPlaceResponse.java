package com.sofly.core.domain.workspace.dto.response;

import com.sofly.core.domain.workspace.entity.SavedPlace;

import java.time.LocalDateTime;

public record SavedPlaceResponse(
        Long id,
        String placeId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String primaryType,
        String photoReference,
        Double rating,
        String googleMapsUri,
        LocalDateTime createdAt
) {
    public static SavedPlaceResponse from(SavedPlace savedPlace) {
        return new SavedPlaceResponse(
                savedPlace.getId(),
                savedPlace.getPlaceId(),
                savedPlace.getName(),
                savedPlace.getAddress(),
                savedPlace.getLatitude(),
                savedPlace.getLongitude(),
                savedPlace.getPrimaryType(),
                savedPlace.getPhotoReference(),
                savedPlace.getRating(),
                savedPlace.getGoogleMapsUri(),
                savedPlace.getCreatedAt()
        );
    }
}
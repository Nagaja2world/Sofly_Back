package com.sofly.supply.application.dto;

import java.util.List;

public record PlacesResponse(
        List<Place> places
) {
    public record Place(
            String id,
            DisplayName displayName,
            String primaryType,
            String formattedAddress,
            Location location,
            Double rating,
            Integer userRatingCount,
            LocalizedText editorialSummary,
            String websiteUri,
            String nationalPhoneNumber,
            String googleMapsUri,
            String businessStatus,
            String priceLevel,
            List<Photo> photos,
            List<AddressComponent> addressComponents
    ) {}

    public record AddressComponent(
            String longText,
            String shortText,
            List<String> types,
            String languageCode
    ) {}

    public record LocalizedText(String text, String languageCode) {}
    public record DisplayName(String text, String languageCode) {}
    public record Location(Double latitude, Double longitude) {}
    public record Photo(String name, Integer widthPx, Integer heightPx) {}
    public record PhotoMedia(String name, String photoUri) {}
}

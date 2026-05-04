package com.sofly.core.domain.place.service;

import com.sofly.core.global.client.SupplyClient;
import com.sofly.core.global.client.dto.PlacesResponse;
import com.sofly.core.global.client.dto.PlacesResponse.PhotoMedia;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final SupplyClient supplyClient;

    public PlacesResponse searchPlaces(String text) {
        try {
            return normalizePlacesResponse(supplyClient.searchPlace(text));
        } catch (FeignException e) {
            throw new SoflyException(ErrorCode.SUPPLY_SERVICE_ERROR, e.getMessage());
        }
    }

    public PhotoMedia getPlacePhoto(String name, int maxWidthPx) {
        try {
            return supplyClient.getPlacePhoto(name, maxWidthPx);
        } catch (FeignException e) {
            throw new SoflyException(ErrorCode.SUPPLY_SERVICE_ERROR, e.getMessage());
        }
    }

    private PlacesResponse normalizePlacesResponse(PlacesResponse response) {
        if (response == null || response.places() == null) {
            return new PlacesResponse(List.of());
        }
        return response;
    }
}

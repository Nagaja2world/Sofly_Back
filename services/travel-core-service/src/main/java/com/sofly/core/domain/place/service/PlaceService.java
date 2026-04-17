package com.sofly.core.domain.place.service;

import com.sofly.core.global.client.SupplyClient;
import com.sofly.core.global.client.dto.PlacesResponse;
import com.sofly.core.global.client.dto.PlacesResponse.PhotoMedia;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final SupplyClient supplyClient;

    public PlacesResponse searchPlaces(String text) {
        try {
            return supplyClient.searchPlace(text);
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
}

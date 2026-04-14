package com.sofly.supply.application.port.outbound;

import com.sofly.supply.application.dto.PlacesResponse;

import java.util.Optional;

public interface PlaceInfoPort {
    Optional<PlacesResponse> searchText(String text);
}

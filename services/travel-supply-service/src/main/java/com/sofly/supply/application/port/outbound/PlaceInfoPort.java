package com.sofly.supply.application.port.outbound;

import com.sofly.supply.application.dto.PlaceInfo;

public interface PlaceInfoPort {
    PlaceInfo fetchPlaceInfo(String hotelName, String cityCode);
}

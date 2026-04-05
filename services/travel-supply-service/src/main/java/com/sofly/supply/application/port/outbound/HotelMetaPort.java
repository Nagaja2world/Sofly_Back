package com.sofly.supply.application.port.outbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.dto.HotelDestination;
import com.sofly.supply.application.dto.HotelOptionsRequest;
import com.sofly.supply.application.dto.HotelSortOption;

import java.util.List;

public interface HotelMetaPort {
    List<HotelDestination> searchDestination(String query);
    List<HotelSortOption> getSortBy(HotelOptionsRequest request);
    JsonNode getFilter(HotelOptionsRequest request);
}

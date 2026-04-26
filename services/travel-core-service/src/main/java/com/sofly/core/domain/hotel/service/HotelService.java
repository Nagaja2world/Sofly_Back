package com.sofly.core.domain.hotel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.core.global.client.SupplyClient;
import com.sofly.core.global.client.dto.HotelDestination;
import com.sofly.core.global.client.dto.HotelOptionsRequest;
import com.sofly.core.global.client.dto.HotelSearchRequest;
import com.sofly.core.global.client.dto.HotelSortOption;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final SupplyClient supplyClient;

    public JsonNode searchHotels(String supplier, HotelSearchRequest request) {
        try {
            return supplyClient.searchHotels(supplier, request);
        } catch (FeignException e) {
            throw new SoflyException(ErrorCode.SUPPLY_SERVICE_ERROR, e.getMessage());
        }
    }

    public List<HotelDestination> searchHotelDestinations(String query) {
        try {
            return supplyClient.searchHotelDestinations(query);
        } catch (FeignException e) {
            throw new SoflyException(ErrorCode.SUPPLY_SERVICE_ERROR, e.getMessage());
        }
    }

    public List<HotelSortOption> getHotelSortOptions(HotelOptionsRequest request) {
        try {
            return supplyClient.getHotelSortOptions(request);
        } catch (FeignException e) {
            throw new SoflyException(ErrorCode.SUPPLY_SERVICE_ERROR, e.getMessage());
        }
    }

    public JsonNode getHotelFilterOptions(HotelOptionsRequest request) {
        try {
            return supplyClient.getHotelFilterOptions(request);
        } catch (FeignException e) {
            throw new SoflyException(ErrorCode.SUPPLY_SERVICE_ERROR, e.getMessage());
        }
    }
}

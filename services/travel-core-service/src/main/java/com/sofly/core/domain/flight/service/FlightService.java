package com.sofly.core.domain.flight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.core.global.client.SupplyClient;
import com.sofly.core.global.client.dto.FlightDestination;
import com.sofly.core.global.client.dto.FlightSearchRequest;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final SupplyClient supplyClient;

    public JsonNode searchFlights(String supplier, FlightSearchRequest request) {
        try {
            return supplyClient.searchFlights(supplier, request);
        } catch (FeignException e) {
            throw new SoflyException(ErrorCode.SUPPLY_SERVICE_ERROR, e.getMessage());
        }
    }

    public List<FlightDestination> searchFlightDestinations(String query, String languageCode) {
        try {
            return supplyClient.searchFlightDestinations(query, languageCode);
        } catch (FeignException e) {
            throw new SoflyException(ErrorCode.SUPPLY_SERVICE_ERROR, e.getMessage());
        }
    }

    public JsonNode getFlightDetails(String supplier, String token, String currencyCode) {
        try {
            return supplyClient.getFlightDetails(supplier, token, currencyCode);
        } catch (FeignException e) {
            throw new SoflyException(ErrorCode.SUPPLY_SERVICE_ERROR, e.getMessage());
        }
    }
}

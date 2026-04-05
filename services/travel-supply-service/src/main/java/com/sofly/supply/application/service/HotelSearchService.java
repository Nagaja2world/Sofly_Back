package com.sofly.supply.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.adapter.outbound.google.GooglePlacesClient;
import com.sofly.supply.adapter.outbound.google.PlaceInfo;
import com.sofly.supply.adapter.outbound.rapidapi.hotels.BookingComDestinationClient;
import com.sofly.supply.application.dto.HotelDestination;
import com.sofly.supply.application.dto.HotelOptionsRequest;
import com.sofly.supply.application.dto.HotelSearchRequest;
import com.sofly.supply.application.dto.HotelSortOption;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelSearchService {

    private final SupplierRouter router;
    private final GooglePlacesClient googlePlacesClient;
    private final BookingComDestinationClient destinationClient;

    public HotelSearchService(SupplierRouter router, GooglePlacesClient googlePlacesClient,
                               BookingComDestinationClient destinationClient) {
        this.router = router;
        this.googlePlacesClient = googlePlacesClient;
        this.destinationClient = destinationClient;
    }

    public JsonNode search(String supplier, HotelSearchRequest request) {
        return router.selectHotelSupplier(supplier).searchHotelsByCity(request);
    }

    public List<HotelDestination> searchDestination(String query) {
        return destinationClient.searchDestination(query);
    }

    public List<HotelSortOption> getSortBy(HotelOptionsRequest request) {
        return destinationClient.getSortBy(request);
    }

    public JsonNode getFilter(HotelOptionsRequest request) {
        return destinationClient.getFilter(request);
    }

    public PlaceInfo getPlaceInfo(String hotelName, String cityCode) {
        return googlePlacesClient.fetchPlaceInfo(hotelName, cityCode);
    }
}

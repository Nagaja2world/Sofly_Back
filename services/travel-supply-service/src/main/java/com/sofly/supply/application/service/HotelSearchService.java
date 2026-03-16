package com.sofly.supply.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.adapter.outbound.google.GooglePlacesClient;
import com.sofly.supply.adapter.outbound.google.PlaceInfo;
import com.sofly.supply.application.dto.HotelSearchResult;
import com.sofly.supply.application.port.outbound.HotelSupplierPort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HotelSearchService {

    private final HotelSupplierPort hotelSupplierPort;
    private final GooglePlacesClient googlePlacesClient;

    public HotelSearchService(HotelSupplierPort hotelSupplierPort, GooglePlacesClient googlePlacesClient) {
        this.hotelSupplierPort = hotelSupplierPort;
        this.googlePlacesClient = googlePlacesClient;
    }

    public List<HotelSearchResult> search(String cityCode, String checkIn, String checkOut, int adults, int roomQuantity) {
        JsonNode amadeusResponse = hotelSupplierPort.searchHotelsByCity(cityCode, checkIn, checkOut, adults, roomQuantity);

        List<HotelSearchResult> results = new ArrayList<>();

        if (amadeusResponse == null || !amadeusResponse.has("data")) {
            return results;
        }

        for (JsonNode item : amadeusResponse.get("data")) {
            JsonNode hotel = item.get("hotel");

            String hotelId   = hotel.has("hotelId")   ? hotel.get("hotelId").asText()   : null;
            String hotelName = hotel.has("name")       ? hotel.get("name").asText()       : null;
            String city      = hotel.has("cityCode")   ? hotel.get("cityCode").asText()   : cityCode;
            Double lat       = hotel.has("latitude")   ? hotel.get("latitude").asDouble() : null;
            Double lng       = hotel.has("longitude")  ? hotel.get("longitude").asDouble(): null;
            JsonNode offers  = item.get("offers");

            // Google Places로 평점·리뷰 수·사진 보완 (실패해도 전체 응답에 영향 없음)
            PlaceInfo placeInfo = (hotelName != null)
                    ? googlePlacesClient.fetchPlaceInfo(hotelName, city)
                    : null;

            results.add(new HotelSearchResult(hotelId, hotelName, city, lat, lng, offers, placeInfo));
        }

        return results;
    }
}

package com.sofly.supply.adapter.inbound.rest;

import com.sofly.supply.adapter.outbound.google.GooglePlacesClient;
import com.sofly.supply.adapter.outbound.google.PlaceInfo;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@RestController
@RequestMapping("/supply/places")
public class GooglePlacesController {

    private final GooglePlacesClient googlePlacesClient;

    public GooglePlacesController(GooglePlacesClient googlePlacesClient) {
        this.googlePlacesClient = googlePlacesClient;
    }

    @GetMapping
    public PlaceInfo search(
            @RequestParam String hotelName,
            @RequestParam String cityCode
    ) {
        return googlePlacesClient.fetchPlaceInfo(hotelName, cityCode);
    }
}

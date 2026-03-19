package com.sofly.supply.adapter.inbound.rest;

import com.sofly.supply.adapter.outbound.google.PlaceInfo;
import com.sofly.supply.application.dto.HotelSearchResult;
import com.sofly.supply.application.service.HotelSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/supply/hotels")
public class HotelSearchController {

    private final HotelSearchService hotelSearchService;

    public HotelSearchController(HotelSearchService hotelSearchService) {
        this.hotelSearchService = hotelSearchService;
    }

    @GetMapping("/offers")
    public List<HotelSearchResult> offers(
            @RequestParam String cityCode,
            @RequestParam java.time.LocalDate checkIn,
            @RequestParam java.time.LocalDate checkOut,
            @RequestParam(defaultValue = "1") int adults,
            @RequestParam(defaultValue = "1") int roomQuantity,
            @RequestParam(required = false) String supplier
    ) {
        return hotelSearchService.search(supplier, cityCode, checkIn, checkOut, adults, roomQuantity);
    }

    @GetMapping("/place-info")
    public PlaceInfo placeInfo(
            @RequestParam String hotelName,
            @RequestParam String cityCode
    ) {
        return hotelSearchService.getPlaceInfo(hotelName, cityCode);
    }
}

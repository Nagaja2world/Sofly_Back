package com.sofly.supply.adapter.inbound.rest;

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
            @RequestParam String cityCode,                              // IATA 도시 코드 (예: PAR, NYC, ICN)
            @RequestParam java.time.LocalDate checkIn,                               // 체크인 날짜 (yyyy-MM-dd)
            @RequestParam java.time.LocalDate checkOut,                              // 체크아웃 날짜 (yyyy-MM-dd)
            @RequestParam(defaultValue = "1") int adults,
            @RequestParam(defaultValue = "1") int roomQuantity
    ) {
        return hotelSearchService.search(cityCode, checkIn, checkOut, adults, roomQuantity);
    }
}

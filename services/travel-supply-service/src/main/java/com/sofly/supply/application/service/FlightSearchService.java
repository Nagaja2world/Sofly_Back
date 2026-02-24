package com.sofly.supply.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FlightSearchService {

    private final FlightSupplierPort flightSupplierPort;

    // 나중에 여러 provider로 라우팅할 때 쓸 값(지금은 1개라 참고용)
    @Value("${app.suppliers.default:amadeus}")
    private String defaultSupplier;

    public FlightSearchService(FlightSupplierPort flightSupplierPort) {
        this.flightSupplierPort = flightSupplierPort;
    }

    public JsonNode search(String origin, String dest, String date, int adults, int max) {
        // 지금은 1개 provider만 연결. (추후 registry/router로 확장)
        return flightSupplierPort.searchFlightOffers(origin, dest, date, adults, max);
    }
}
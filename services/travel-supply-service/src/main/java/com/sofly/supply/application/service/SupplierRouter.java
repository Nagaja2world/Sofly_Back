package com.sofly.supply.application.service;

import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import com.sofly.supply.application.port.outbound.HotelSupplierPort;
import com.sofly.supply.bootstrap.SupplierRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SupplierRouter {

    private final SupplierRegistry registry;

    @Value("${app.suppliers.default:amadeus}")
    private String defaultSupplier;

    public SupplierRouter(SupplierRegistry registry) {
        this.registry = registry;
    }

    public FlightSupplierPort selectFlightSupplier(String requestedSupplier) {
        String key = (requestedSupplier == null || requestedSupplier.isBlank())
                ? defaultSupplier
                : requestedSupplier;
        return registry.getFlightSupplier(key);
    }

    public HotelSupplierPort selectHotelSupplier(String requestedSupplier) {
        String key = (requestedSupplier == null || requestedSupplier.isBlank())
                ? defaultSupplier
                : requestedSupplier;
        return registry.getHotelSupplier(key);
    }
}
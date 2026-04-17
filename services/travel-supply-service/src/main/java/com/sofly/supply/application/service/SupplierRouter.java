package com.sofly.supply.application.service;

import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import com.sofly.supply.application.port.outbound.HotelSupplierPort;
import com.sofly.supply.bootstrap.SupplierRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class SupplierRouter {

    private final SupplierRegistry registry;

    @Value("${app.suppliers.default:booking}")
    private String defaultSupplier;

    public SupplierRouter(SupplierRegistry registry) {
        this.registry = registry;
    }

    public FlightSupplierPort selectFlightSupplier(String requestedSupplier) {
        return select(requestedSupplier, registry::getFlightSupplier);
    }

    public HotelSupplierPort selectHotelSupplier(String requestedSupplier) {
        return select(requestedSupplier, registry::getHotelSupplier);
    }

    private <T> T select(String requestedSupplier, Function<String, T> getter) {
        String key = (requestedSupplier == null || requestedSupplier.isBlank())
                ? defaultSupplier
                : requestedSupplier;
        return getter.apply(key);
    }
}
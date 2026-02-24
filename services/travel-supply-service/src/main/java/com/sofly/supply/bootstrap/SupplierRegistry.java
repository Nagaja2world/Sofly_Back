package com.sofly.supply.bootstrap;

import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SupplierRegistry {

    private final Map<String, FlightSupplierPort> flightSuppliers;

    public SupplierRegistry(List<FlightSupplierPort> suppliers) {
        this.flightSuppliers = suppliers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        s -> s.supplierKey().toLowerCase(),
                        Function.identity()
                ));
    }

    public FlightSupplierPort getFlightSupplier(String supplierKey) {
        FlightSupplierPort supplier = flightSuppliers.get(supplierKey.toLowerCase());
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown flight supplier: " + supplierKey);
        }
        return supplier;
    }

    public Map<String, FlightSupplierPort> allFlightSuppliers() {
        return flightSuppliers;
    }
}
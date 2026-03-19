package com.sofly.supply.bootstrap;

import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import com.sofly.supply.application.port.outbound.HotelSupplierPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SupplierRegistry {

    private final Map<String, FlightSupplierPort> flightSuppliers;
    private final Map<String, HotelSupplierPort> hotelSuppliers;

    public SupplierRegistry(List<FlightSupplierPort> flightSuppliers,
                            List<HotelSupplierPort> hotelSuppliers) {
        this.flightSuppliers = flightSuppliers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        s -> s.supplierKey().toLowerCase(),
                        Function.identity()
                ));
        this.hotelSuppliers = hotelSuppliers.stream()
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

    public HotelSupplierPort getHotelSupplier(String supplierKey) {
        HotelSupplierPort supplier = hotelSuppliers.get(supplierKey.toLowerCase());
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown hotel supplier: " + supplierKey);
        }
        return supplier;
    }

    public Map<String, HotelSupplierPort> allHotelSuppliers() {
        return hotelSuppliers;
    }
}
package com.sofly.supply.application.port.outbound;

import com.sofly.supply.application.dto.FlightDestination;

import java.util.List;

public interface FlightMetaPort {
    List<FlightDestination> searchDestinations(String query, String languageCode);
}

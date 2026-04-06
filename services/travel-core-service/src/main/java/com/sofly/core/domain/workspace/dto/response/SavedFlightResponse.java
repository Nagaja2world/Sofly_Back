package com.sofly.core.domain.workspace.dto.response;

import com.sofly.core.domain.workspace.entity.SavedFlight;
import com.sofly.core.domain.workspace.entity.SavedFlight.FlightType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SavedFlightResponse {

    private Long id;
    private String flightNumber;
    private String airline;
    private String departureAirport;
    private String arrivalAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String duration;
    private Integer price;
    private FlightType flightType;

    public static SavedFlightResponse from(SavedFlight flight) {
        return SavedFlightResponse.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .airline(flight.getAirline())
                .departureAirport(flight.getDepartureAirport())
                .arrivalAirport(flight.getArrivalAirport())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .duration(flight.getDuration())
                .price(flight.getPrice())
                .flightType(flight.getFlightType())
                .build();
    }
}

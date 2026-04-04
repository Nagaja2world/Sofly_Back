package com.sofly.core.domain.workspace.dto.request;

import com.sofly.core.domain.workspace.entity.SavedFlight.FlightType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SaveFlightRequest {

    @NotBlank
    private String flightNumber;

    @NotBlank
    private String airline;

    @NotBlank
    private String departureAirport;

    @NotBlank
    private String arrivalAirport;

    @NotNull
    private LocalDateTime departureTime;

    @NotNull
    private LocalDateTime arrivalTime;

    private String duration;

    private Integer price;

    @NotNull
    private FlightType flightType;
}

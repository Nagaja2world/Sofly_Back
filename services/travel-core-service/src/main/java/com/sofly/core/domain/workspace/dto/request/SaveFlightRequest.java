package com.sofly.core.domain.workspace.dto.request;

import com.sofly.core.domain.workspace.entity.SavedFlight.FlightType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

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
    private ZonedDateTime departureTime;

    @NotNull
    private ZonedDateTime arrivalTime;

    private String duration;

    private Integer price;

    @NotNull
    private FlightType flightType;
}

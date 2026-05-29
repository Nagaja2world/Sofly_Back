package com.sofly.core.domain.workspace.dto.request;

import com.sofly.core.domain.workspace.entity.SavedFlight.FlightType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class SaveFlightRequest {

    @NotBlank
    private String flightNumber;

    @NotBlank
    private String airline;

    private String airlineLogo;
    private String planeType;
    private String cabinClass;

    @NotBlank
    private String departureAirport;

    private String departureCity;
    private String departureCountry;
    private String departureTerminal;

    @NotBlank
    private String arrivalAirport;

    private String arrivalCity;
    private String arrivalCountry;
    private String arrivalTerminal;

    @NotNull
    private ZonedDateTime departureTime;

    @NotNull
    private ZonedDateTime arrivalTime;

    /** totalTime(초) / 60 → durationMinutes */
    private Integer durationMinutes;

    /** priceBreakdown.total */
    private Integer totalPrice;

    private Integer baseFare;
    private Integer tax;
    private Integer platformFee;
    private String currencyCode;

    private Integer checkedBaggageKg;
    private Integer checkedBaggagePiece;
    private Integer cabinBaggageKg;
    private Boolean personalItemIncluded;

    private String bookingToken;
    private String offerReference;
    private String deepLinkUrl;

    @NotNull
    private FlightType flightType;
}

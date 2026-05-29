package com.sofly.core.domain.workspace.dto.request;

import com.sofly.core.domain.workspace.entity.SavedFlight.FlightType;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class UpdateFlightRequest {

    private String flightNumber;
    private String airline;
    private String airlineLogo;
    private String planeType;
    private String cabinClass;

    private String departureAirport;
    private String departureCity;
    private String departureCountry;
    private String departureTerminal;

    private String arrivalAirport;
    private String arrivalCity;
    private String arrivalCountry;
    private String arrivalTerminal;

    private ZonedDateTime departureTime;
    private ZonedDateTime arrivalTime;
    private Integer durationMinutes;

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

    private FlightType flightType;
}

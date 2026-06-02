package com.sofly.core.domain.workspace.dto.response;

import com.sofly.core.domain.workspace.entity.SavedFlight;
import com.sofly.core.domain.workspace.entity.SavedFlight.FlightType;
import lombok.Builder;
import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Getter
@Builder
public class SavedFlightResponse {

    private Long id;

    // 항공편 기본
    private String flightNumber;
    private String airline;
    private String airlineLogo;
    private String planeType;
    private String cabinClass;

    // 출발 공항
    private String departureAirport;
    private String departureCity;
    private String departureCountry;
    private String departureTerminal;

    // 도착 공항
    private String arrivalAirport;
    private String arrivalCity;
    private String arrivalCountry;
    private String arrivalTerminal;

    // 시간
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime departureTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime arrivalTime;
    private Integer durationMinutes;

    // 가격
    private Integer totalPrice;
    private Integer baseFare;
    private Integer tax;
    private Integer platformFee;
    private String currencyCode;

    // 수하물
    private Integer checkedBaggageKg;
    private Integer checkedBaggagePiece;
    private Integer cabinBaggageKg;
    private Boolean personalItemIncluded;

    // 예약
    private String bookingToken;
    private String offerReference;
    private String deepLinkUrl;

    private FlightType flightType;

    public static SavedFlightResponse from(SavedFlight flight) {
        return SavedFlightResponse.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .airline(flight.getAirline())
                .airlineLogo(flight.getAirlineLogo())
                .planeType(flight.getPlaneType())
                .cabinClass(flight.getCabinClass())
                .departureAirport(flight.getDepartureAirport())
                .departureCity(flight.getDepartureCity())
                .departureCountry(flight.getDepartureCountry())
                .departureTerminal(flight.getDepartureTerminal())
                .arrivalAirport(flight.getArrivalAirport())
                .arrivalCity(flight.getArrivalCity())
                .arrivalCountry(flight.getArrivalCountry())
                .arrivalTerminal(flight.getArrivalTerminal())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .durationMinutes(flight.getDurationMinutes())
                .totalPrice(flight.getTotalPrice())
                .baseFare(flight.getBaseFare())
                .tax(flight.getTax())
                .platformFee(flight.getPlatformFee())
                .currencyCode(flight.getCurrencyCode())
                .checkedBaggageKg(flight.getCheckedBaggageKg())
                .checkedBaggagePiece(flight.getCheckedBaggagePiece())
                .cabinBaggageKg(flight.getCabinBaggageKg())
                .personalItemIncluded(flight.getPersonalItemIncluded())
                .bookingToken(flight.getBookingToken())
                .offerReference(flight.getOfferReference())
                .deepLinkUrl(flight.getDeepLinkUrl())
                .flightType(flight.getFlightType())
                .build();
    }
}

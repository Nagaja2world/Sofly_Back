package com.sofly.core.domain.conquest.dto.response;

import lombok.Builder;
import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Getter
@Builder
public class TripRouteResponse {

    private Long flightId;
    private Long workspaceId;
    private String workspaceTitle;

    private String departureAirport;
    private String departureCity;
    private String departureCountryCode;
    private double departureLat;
    private double departureLng;

    private String arrivalAirport;
    private String arrivalCity;
    private String arrivalCountryCode;
    private double arrivalLat;
    private double arrivalLng;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime departureTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime arrivalTime;

    private String airline;
    private String flightNumber;
    private double distanceKm;

    // 국가 간 이동: INTERNATIONAL, 동일 국가 내: DOMESTIC
    private RouteType routeType;

    public enum RouteType { INTERNATIONAL, DOMESTIC }
}

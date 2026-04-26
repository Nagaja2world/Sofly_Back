package com.sofly.core.global.client.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class HotelSearchRequest {

    private String destId;
    private String searchType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate arrivalDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;

    private Integer adults;
    private String childrenAge;
    private Integer roomQty;
    private Integer pageNumber;
    private Float priceMin;
    private Float priceMax;
    private String sortBy;
    private String categoriesFilter;
    private Units units;
    private TemperatureUnit temperatureUnit;
    private String languageCode;
    private String currencyCode;
    private String location;

    public enum Units {
        METRIC, IMPERIAL
    }

    public enum TemperatureUnit {
        CELSIUS, FAHRENHEIT
    }
}

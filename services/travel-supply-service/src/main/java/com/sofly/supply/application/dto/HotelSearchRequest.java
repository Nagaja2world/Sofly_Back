package com.sofly.supply.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelSearchRequest {

    @Schema(defaultValue = "267199", required = true)
    private Float destId;

    @Schema(defaultValue = "LANDMARK", required = true)
    private String searchType;

    @Schema(defaultValue = "2026-04-06", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate arrivalDate; //checkIn

    @Schema(defaultValue = "2026-04-08", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate; //checkOut

    @Schema(defaultValue = "1")
    private Integer adults;

    @Schema(defaultValue = "0")
    private String childrenAge;

    @Schema(defaultValue = "1")
    private Integer roomQty;

    @Schema(defaultValue = "1")
    private Integer pageNumber;

    @Schema(defaultValue = "0")
    private Float priceMin;

    @Schema(defaultValue = "0")
    private Float priceMax;

    @Schema(defaultValue = "price")
    private String sortBy;

    @Schema(defaultValue = "popular")
    private String categoriesFilter;

    @Schema(defaultValue = "METRIC")
    private Units units;

    @Schema(defaultValue = "Celsius")
    private TemperatureUnit temperatureUnit;

    @Schema(defaultValue = "ko")
    private String languageCode;

    @Schema(defaultValue = "KRW")
    private String currencyCode;

    private String location;

    public enum Units{
        METRIC,
        IMPERIAL;
    }

    public enum TemperatureUnit{
        CELSIUS("c"),
        FAHRENHEIT("f");

        private final String value;

        TemperatureUnit(String value) { this.value = value; }
        TemperatureUnit() { this.value = this.name(); }

        public String getValue() { return value; }
    }
}

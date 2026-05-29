package com.sofly.core.global.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class HotelDetailsRequest {

    @Schema(defaultValue = "191605", requiredMode = Schema.RequiredMode.REQUIRED)
    private String hotelId;

    @Schema(defaultValue = "2026-04-06", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate arrivalDate;

    @Schema(defaultValue = "2026-04-08", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;

    @Schema(defaultValue = "1")
    private Integer adults;

    @Schema(defaultValue = "0")
    private String childrenAge;

    @Schema(defaultValue = "1")
    private Integer roomQty;

    @Schema(defaultValue = "METRIC")
    private HotelSearchRequest.Units units;

    @Schema(defaultValue = "Celsius")
    private HotelSearchRequest.TemperatureUnit temperatureUnit;

    @Schema(defaultValue = "ko")
    private String languageCode;

    @Schema(defaultValue = "KRW")
    private String currencyCode;
}

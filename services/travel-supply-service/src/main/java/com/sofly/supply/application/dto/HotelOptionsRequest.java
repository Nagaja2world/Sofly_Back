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
public class HotelOptionsRequest {

    @Schema(defaultValue = "-2092174", required = true)
    private String destId;

    @Schema(defaultValue = "CITY", required = true)
    private String searchType;

    @Schema(defaultValue = "2026-04-06", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate arrivalDate;

    @Schema(defaultValue = "2026-04-08", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;

    @Schema(defaultValue = "1")
    private Integer adults;

    private String childrenAge;

    @Schema(defaultValue = "1")
    private Integer roomQty;

    // getFilter 전용 (초기 요청 시 비워두면 됨)
    private String categoriesFilter;
}

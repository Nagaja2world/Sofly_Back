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
public class HotelOptionsRequest {

    private String destId;
    private String searchType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate arrivalDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;

    private Integer adults;
    private String childrenAge;
    private Integer roomQty;
    private String categoriesFilter;
}

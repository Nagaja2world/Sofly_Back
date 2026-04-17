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
public class FlightSearchRequest {

    private String fromId;
    private String toId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate returnDate;

    @Builder.Default
    private int adults = 2;
    private String currencyCode;

    private StopsType stops;
    private SortType sort;
    private CabinClass cabinClass;
    private String childrenAge;

    @Builder.Default
    private Integer pageNo = 1;

    public enum StopsType {
        none, ZERO, ONE, TWO
    }

    public enum SortType {
        BEST, CHEAPEST, FASTEST
    }

    public enum CabinClass {
        ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST
    }
}

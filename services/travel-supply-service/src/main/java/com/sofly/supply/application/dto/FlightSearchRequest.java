package com.sofly.supply.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchRequest {

    // 공통 필수
    private String fromId;        // booking: fromId, Amadeus: originLocationCode
    private String toId;          // booking: toId,   Amadeus: destinationLocationCode
    private LocalDate departDate; // 출발일

    // 공통 선택
    private LocalDate returnDate;
    @Builder.Default
    private int adults = 1;
    private String currencyCode;

    // Amadeus 전용
    @Builder.Default
    private int max = 10;

    // booking 전용
    private StopsType stops;
    private SortType sort;
    private CabinClass cabinClass;
    private String childrenAge;
    @Builder.Default
    private Integer pageNo = 1;


    //booking enum
    public enum StopsType {
        none, // 상관없음
        ZERO("0"),
        ONE("1"),
        TWO("2");

        private final String value;

        StopsType(String value) { this.value = value; }
        StopsType() { this.value = this.name(); }

        public String getValue() { return value; }
    }

    public enum SortType {
        BEST, CHEAPEST, FASTEST
    }

    public enum CabinClass {
        ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST
    }
}

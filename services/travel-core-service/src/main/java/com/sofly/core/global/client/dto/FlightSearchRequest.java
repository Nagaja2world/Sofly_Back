package com.sofly.core.global.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class FlightSearchRequest {

    @Schema(defaultValue = "ICN.AIRPORT")
    private String fromId;
    @Schema(defaultValue = "CEB.AIRPORT")
    private String toId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(defaultValue = "2026-04-04")
    private LocalDate departDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(defaultValue = "2026-04-06")
    private LocalDate returnDate;

    @Builder.Default
    @Schema(defaultValue = "2")
    private int adults = 2;
    @Schema(defaultValue = "KRW")
    private String currencyCode;

    @Schema(defaultValue = "none")
    private StopsType stops;
    @Schema(defaultValue = "BEST")
    private SortType sort;
    @Schema(defaultValue = "ECONOMY")
    private CabinClass cabinClass;
    private String childrenAge;

    @Builder.Default
    @Schema(defaultValue = "1")
    private Integer pageNo = 1;

    @Schema(description = "항공사 코드 필터 (예: KE, OZ). 미입력 시 전체 반환")
    private List<String> airlines;
    @Schema(description = "다음 페이지 커서 (이전 응답의 nextPageCursor 값). 항공사 필터 사용 시에만 유효")
    private String cursor;

    public enum StopsType {
        none,
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

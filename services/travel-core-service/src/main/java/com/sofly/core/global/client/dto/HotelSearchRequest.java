package com.sofly.core.global.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "목적지 ID (/api/v1/hotels/destinations 로 조회)", defaultValue = "267199", requiredMode = Schema.RequiredMode.REQUIRED)
    private String destId;

    @Schema(description = "검색 타입 (/api/v1/hotels/destinations 응답의 dest_type)", defaultValue = "LANDMARK", requiredMode = Schema.RequiredMode.REQUIRED)
    private String searchType;

    @Schema(description = "체크인 날짜 (yyyy-MM-dd)", defaultValue = "2026-06-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate arrivalDate;

    @Schema(description = "체크아웃 날짜 (yyyy-MM-dd)", defaultValue = "2026-06-03", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;

    @Schema(description = "성인 인원 수", defaultValue = "1")
    private Integer adults;

    @Schema(description = "아동 나이 (쉼표 구분, 예: 5,7)", defaultValue = "0")
    private String childrenAge;

    @Schema(description = "객실 수", defaultValue = "1")
    private Integer roomQty;

    @Schema(description = "페이지 번호", defaultValue = "1")
    private Integer pageNumber;

    @Schema(description = "최소 가격", defaultValue = "0")
    private Float priceMin;

    @Schema(description = "최대 가격 (0이면 제한 없음)", defaultValue = "0")
    private Float priceMax;

    @Schema(description = "정렬 기준 (/api/v1/hotels/sort-options 로 목록 조회)", defaultValue = "price")
    private String sortBy;

    @Schema(description = "필터 (/api/v1/hotels/filter-options 로 목록 조회)", defaultValue = "popular")
    private String categoriesFilter;

    @Schema(description = "단위 (METRIC / IMPERIAL)", defaultValue = "METRIC")
    private Units units;

    @Schema(description = "온도 단위 (CELSIUS / FAHRENHEIT)", defaultValue = "CELSIUS")
    private TemperatureUnit temperatureUnit;

    @Schema(description = "언어 코드", defaultValue = "ko")
    private String languageCode;

    @Schema(description = "통화 코드", defaultValue = "KRW")
    private String currencyCode;

    private String location;

    public enum Units {
        METRIC, IMPERIAL
    }

    public enum TemperatureUnit {
        CELSIUS, FAHRENHEIT
    }
}

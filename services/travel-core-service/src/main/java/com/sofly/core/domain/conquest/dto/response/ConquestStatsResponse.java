package com.sofly.core.domain.conquest.dto.response;

import com.sofly.core.domain.conquest.enums.Continent;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ConquestStatsResponse {

    private static final int TOTAL_COUNTRY_COUNT = 195;

    private int visitedCountryCount;
    private int totalCountryCount;
    private double visitedCountryPercentage;
    private int visitedCityCount;
    private int totalTravelDays;
    private double totalDistanceKm;
    private List<ContinentStats> continentStats;

    public static ConquestStatsResponse of(
            int visitedCountryCount,
            int visitedCityCount,
            int totalTravelDays,
            double totalDistanceKm,
            List<ContinentStats> continentStats
    ) {
        double percentage = Math.round((double) visitedCountryCount / TOTAL_COUNTRY_COUNT * 1000.0) / 10.0;
        return ConquestStatsResponse.builder()
                .visitedCountryCount(visitedCountryCount)
                .totalCountryCount(TOTAL_COUNTRY_COUNT)
                .visitedCountryPercentage(percentage)
                .visitedCityCount(visitedCityCount)
                .totalTravelDays(totalTravelDays)
                .totalDistanceKm(totalDistanceKm)
                .continentStats(continentStats)
                .build();
    }

    @Getter
    @Builder
    public static class ContinentStats {
        private Continent continent;
        private String continentName;
        private int visitedCountryCount;
    }
}

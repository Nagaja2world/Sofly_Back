package com.sofly.core.domain.conquest.dto.response;

import com.sofly.core.domain.conquest.entity.VisitedCountry;
import com.sofly.core.domain.conquest.enums.Continent;
import com.sofly.core.domain.conquest.enums.VisitStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VisitedCountryResponse {

    private Long id;
    private String countryCode;
    private String countryName;
    private VisitStatus status;
    private Continent continent;
    private String continentName;
    private int visitCount;

    public static VisitedCountryResponse from(VisitedCountry visitedCountry) {
        return VisitedCountryResponse.builder()
                .id(visitedCountry.getId())
                .countryCode(visitedCountry.getCountryCode())
                .countryName(visitedCountry.getCountryName())
                .status(visitedCountry.getStatus())
                .continent(visitedCountry.getContinent())
                .continentName(visitedCountry.getContinent().getDisplayName())
                .visitCount(visitedCountry.getVisitCount())
                .build();
    }
}

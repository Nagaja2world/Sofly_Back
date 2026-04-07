package com.sofly.core.domain.conquest.dto.response;

import com.sofly.core.domain.conquest.entity.VisitedCity;
import com.sofly.core.domain.conquest.enums.VisitStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VisitedCityResponse {

    private Long id;
    private String cityName;
    private String countryCode;
    private double latitude;
    private double longitude;
    private VisitStatus status;
    private int visitCount;

    public static VisitedCityResponse from(VisitedCity visitedCity) {
        return VisitedCityResponse.builder()
                .id(visitedCity.getId())
                .cityName(visitedCity.getCityName())
                .countryCode(visitedCity.getCountryCode())
                .latitude(visitedCity.getLatitude())
                .longitude(visitedCity.getLongitude())
                .status(visitedCity.getStatus())
                .visitCount(visitedCity.getVisitCount())
                .build();
    }
}

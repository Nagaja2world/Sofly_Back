package com.sofly.core.domain.conquest.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ConquestMapResponse {

    private List<VisitedCountryResponse> countries;
    private List<VisitedCityResponse> cities;
}

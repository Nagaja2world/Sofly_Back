package com.sofly.core.domain.conquest.dto.request;

import com.sofly.core.domain.conquest.enums.VisitStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CityCreateRequest {

    @NotBlank(message = "도시명은 필수입니다.")
    private String cityName;

    @NotBlank(message = "국가 코드는 필수입니다.")
    private String countryCode;

    @NotNull(message = "위도는 필수입니다.")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다.")
    private Double longitude;

    @NotNull(message = "상태값은 필수입니다.")
    private VisitStatus status;
}

package com.sofly.core.domain.conquest.dto.request;

import com.sofly.core.domain.conquest.enums.VisitStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class BulkImportRequest {

    private List<CountryImport> countries;
    private List<CityImport> cities;

    @Getter
    public static class CountryImport {
        @NotBlank
        private String countryCode;

        @NotNull
        private VisitStatus status;
    }

    @Getter
    public static class CityImport {
        @NotBlank
        private String cityName;

        @NotBlank
        private String countryCode;

        @NotNull
        private Double latitude;

        @NotNull
        private Double longitude;

        @NotNull
        private VisitStatus status;
    }
}

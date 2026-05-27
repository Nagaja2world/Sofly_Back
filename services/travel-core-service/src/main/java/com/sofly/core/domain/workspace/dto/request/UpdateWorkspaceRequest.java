package com.sofly.core.domain.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class UpdateWorkspaceRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String destination;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private Integer headcount;

    private String coverImageUrl;

    private String countryCode;
}

package com.sofly.core.domain.workspace.dto.request;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class UpdateWorkspaceRequest {

    private String title;

    private String destination;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer headcount;

    private String coverImageUrl;

    private String countryCode;
}

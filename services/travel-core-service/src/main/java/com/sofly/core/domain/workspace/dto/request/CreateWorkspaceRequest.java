package com.sofly.core.domain.workspace.dto.request;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CreateWorkspaceRequest {

    private String title;

    private String destination;

    private String countryCode;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer headcount;

    private String coverImageUrl;
}

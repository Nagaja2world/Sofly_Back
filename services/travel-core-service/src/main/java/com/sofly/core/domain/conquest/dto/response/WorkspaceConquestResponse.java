package com.sofly.core.domain.conquest.dto.response;

import com.sofly.core.domain.workspace.entity.Workspace;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class WorkspaceConquestResponse {

    private Long id;
    private String title;
    private String destination;
    private String countryCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private String coverImageUrl;
    private int memberCount;

    public static WorkspaceConquestResponse from(Workspace workspace) {
        return WorkspaceConquestResponse.builder()
                .id(workspace.getId())
                .title(workspace.getTitle())
                .destination(workspace.getDestination())
                .countryCode(workspace.getCountryCode())
                .startDate(workspace.getStartDate())
                .endDate(workspace.getEndDate())
                .coverImageUrl(workspace.getCoverImageUrl())
                .memberCount(workspace.getMembers().size())
                .build();
    }
}

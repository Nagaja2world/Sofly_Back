package com.sofly.core.domain.workspace.dto.response;

import com.sofly.core.domain.workspace.entity.Workspace;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class WorkspaceResponse {

    private Long id;
    private String title;
    private String destination;
    private String countryCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer headcount;
    private String coverImageUrl;
    private Long ownerId;
    private int memberCount;

    public static WorkspaceResponse from(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .title(workspace.getTitle())
                .destination(workspace.getDestination())
                .countryCode(workspace.getCountryCode())
                .startDate(workspace.getStartDate())
                .endDate(workspace.getEndDate())
                .headcount(workspace.getHeadcount())
                .coverImageUrl(workspace.getCoverImageUrl())
                .ownerId(workspace.getOwner().getId())
                .memberCount(workspace.getMembers().size())
                .build();
    }
}

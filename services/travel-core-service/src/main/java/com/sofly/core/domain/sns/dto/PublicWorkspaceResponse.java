package com.sofly.core.domain.sns.dto;

import com.sofly.core.domain.schedule.entity.Schedule;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.entity.WorkspaceVisibility;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class PublicWorkspaceResponse {

    private Long id;
    private String title;
    private String destination;
    private String countryCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer headcount;
    private String coverImageUrl;
    private WorkspaceVisibility visibility;
    private AuthorInfo author;
    private long likeCount;
    private long commentCount;
    private Boolean isLiked;
    private ScheduleSummary latestSchedule;
    private LocalDateTime createdAt;

    public static PublicWorkspaceResponse of(Workspace workspace,
                                              long likeCount,
                                              long commentCount,
                                              Boolean isLiked,
                                              Schedule latestSchedule) {
        return PublicWorkspaceResponse.builder()
                .id(workspace.getId())
                .title(workspace.getTitle())
                .destination(workspace.getDestination())
                .countryCode(workspace.getCountryCode())
                .startDate(workspace.getStartDate())
                .endDate(workspace.getEndDate())
                .headcount(workspace.getHeadcount())
                .coverImageUrl(workspace.getCoverImageUrl())
                .visibility(workspace.getVisibility())
                .author(AuthorInfo.from(workspace.getOwner()))
                .likeCount(likeCount)
                .commentCount(commentCount)
                .isLiked(isLiked)
                .latestSchedule(latestSchedule != null ? ScheduleSummary.from(latestSchedule) : null)
                .createdAt(workspace.getCreatedAt())
                .build();
    }
}

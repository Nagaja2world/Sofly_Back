package com.sofly.core.domain.sns.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofly.core.domain.schedule.entity.Schedule;
import com.sofly.core.domain.schedule.entity.ScheduleItem;
import com.sofly.core.domain.schedule.repository.ScheduleRepository;
import com.sofly.core.domain.sns.code.SnsErrorCode;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.workspace.dto.response.WorkspaceResponse;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.entity.WorkspaceMember;
import com.sofly.core.domain.workspace.entity.WorkspaceVisibility;
import com.sofly.core.domain.workspace.exception.WorkspaceException;
import com.sofly.core.domain.workspace.code.WorkspaceErrorCode;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceImportService {

    private final WorkspaceRepository workspaceRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    @Transactional
    public WorkspaceResponse importWorkspace(Long sourceWorkspaceId, Long userId) {
        Workspace source = workspaceRepository.findWithOwnerById(sourceWorkspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        if (source.getVisibility() != WorkspaceVisibility.PUBLIC) {
            throw new SnsException(SnsErrorCode.WORKSPACE_NOT_PUBLIC);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        Workspace newWorkspace = Workspace.builder()
                .title(source.getTitle())
                .destination(source.getDestination())
                .countryCode(source.getCountryCode())
                .startDate(source.getStartDate())
                .endDate(source.getEndDate())
                .headcount(source.getHeadcount())
                .coverImageUrl(source.getCoverImageUrl())
                .owner(user)
                .build();

        WorkspaceMember ownerMember = WorkspaceMember.ofOwner(newWorkspace, user);
        newWorkspace.addMember(ownerMember);
        workspaceRepository.save(newWorkspace);

        scheduleRepository.findTopByWorkspaceIdOrderByVersionDesc(sourceWorkspaceId)
                .ifPresent(sourceSchedule -> copySchedule(sourceSchedule, newWorkspace));

        return WorkspaceResponse.from(newWorkspace);
    }

    private void copySchedule(Schedule source, Workspace newWorkspace) {
        Schedule newSchedule = Schedule.builder()
                .workspace(newWorkspace)
                .title(source.getTitle())
                .version(1)
                .build();

        List<ScheduleItem> copiedItems = source.getItems().stream()
                .map(item -> ScheduleItem.builder()
                        .schedule(newSchedule)
                        .day(item.getDay())
                        .orderIndex(item.getOrderIndex())
                        .category(item.getCategory())
                        .name(item.getName())
                        .address(item.getAddress())
                        .latitude(item.getLatitude())
                        .longitude(item.getLongitude())
                        .placeId(item.getPlaceId())
                        .photoReference(item.getPhotoReference())
                        .visitTime(item.getVisitTime())
                        .memo(item.getMemo())
                        .estimatedCost(item.getEstimatedCost())
                        .build())
                .toList();

        copiedItems.forEach(newSchedule::addItem);
        scheduleRepository.save(newSchedule);
    }
}

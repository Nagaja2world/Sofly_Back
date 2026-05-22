package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.schedule.entity.Schedule;
import com.sofly.core.domain.schedule.repository.ScheduleRepository;
import com.sofly.core.domain.sns.dto.PublicWorkspaceResponse;
import com.sofly.core.domain.sns.repository.WorkspaceCommentRepository;
import com.sofly.core.domain.sns.repository.WorkspaceLikeRepository;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceLikeRepository workspaceLikeRepository;
    private final WorkspaceCommentRepository workspaceCommentRepository;
    private final ScheduleRepository scheduleRepository;

    public Page<PublicWorkspaceResponse> search(String countryCode, String keyword,
                                                 Long viewerIdOrNull, Pageable pageable) {
        Page<Workspace> workspacePage = workspaceRepository.searchPublic(countryCode, keyword, pageable);
        List<Workspace> workspaces = workspacePage.getContent();
        if (workspaces.isEmpty()) return Page.empty(pageable);

        List<Long> ids = workspaces.stream().map(Workspace::getId).toList();
        Map<Long, Long> likeCounts = toMap(workspaceLikeRepository.countByWorkspaceIds(ids));
        Map<Long, Long> commentCounts = toMap(workspaceCommentRepository.countByWorkspaceIds(ids));
        Set<Long> likedByViewer = viewerIdOrNull == null ? Set.of()
                : Set.copyOf(workspaceLikeRepository.findLikedWorkspaceIdsByUserId(viewerIdOrNull, ids));

        List<PublicWorkspaceResponse> responses = workspaces.stream()
                .map(w -> toResponse(w, likeCounts, commentCounts, likedByViewer, viewerIdOrNull))
                .toList();

        return new PageImpl<>(responses, pageable, workspacePage.getTotalElements());
    }

    private PublicWorkspaceResponse toResponse(Workspace workspace,
                                                Map<Long, Long> likeCounts,
                                                Map<Long, Long> commentCounts,
                                                Set<Long> likedByViewer,
                                                Long viewerIdOrNull) {
        Schedule latestSchedule = scheduleRepository
                .findAllWithItemsByWorkspaceId(workspace.getId())
                .stream().findFirst().orElse(null);
        Boolean isLiked = viewerIdOrNull == null ? null : likedByViewer.contains(workspace.getId());
        return PublicWorkspaceResponse.of(
                workspace,
                likeCounts.getOrDefault(workspace.getId(), 0L),
                commentCounts.getOrDefault(workspace.getId(), 0L),
                isLiked,
                latestSchedule);
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }
}

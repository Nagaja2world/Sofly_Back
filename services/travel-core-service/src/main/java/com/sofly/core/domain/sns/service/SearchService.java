package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.schedule.repository.ScheduleRepository;
import com.sofly.core.domain.sns.dto.PublicWorkspaceResponse;
import com.sofly.core.domain.sns.dto.ScheduleSummary;
import com.sofly.core.domain.sns.dto.TrendingDestinationResponse;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.sns.repository.WorkspaceCommentRepository;
import com.sofly.core.domain.sns.repository.WorkspaceLikeRepository;
import com.sofly.core.domain.workspace.code.WorkspaceErrorCode;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.exception.WorkspaceException;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sofly.core.domain.sns.code.SnsErrorCode.WORKSPACE_NOT_PUBLIC;

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
        String normalizedCountryCode = normalizeCountryCode(countryCode);
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<Workspace> workspacePage = normalizedKeyword == null
                ? workspaceRepository.searchPublic(normalizedCountryCode, pageable)
                : workspaceRepository.searchPublicByKeyword(normalizedCountryCode, normalizedKeyword, pageable);
        List<Workspace> workspaces = workspacePage.getContent();
        if (workspaces.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, workspacePage.getTotalElements());
        }

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

    public ScheduleSummary getLatestSchedule(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getVisibility() == com.sofly.core.domain.workspace.entity.WorkspaceVisibility.PRIVATE) {
            throw new SnsException(WORKSPACE_NOT_PUBLIC);
        }
        return scheduleRepository.findAllWithItemsByWorkspaceId(workspaceId)
                .stream()
                .findFirst()
                .map(ScheduleSummary::from)
                .orElse(null);
    }

    private PublicWorkspaceResponse toResponse(Workspace workspace,
                                                Map<Long, Long> likeCounts,
                                                Map<Long, Long> commentCounts,
                                                Set<Long> likedByViewer,
                                                Long viewerIdOrNull) {
        Boolean isLiked = viewerIdOrNull == null ? null : likedByViewer.contains(workspace.getId());
        return PublicWorkspaceResponse.of(
                workspace,
                likeCounts.getOrDefault(workspace.getId(), 0L),
                commentCounts.getOrDefault(workspace.getId(), 0L),
                isLiked);
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    /** 검색 키워드를 LIKE 와일드카드 이스케이프 처리 후 반환. 공백/null이면 null. */
    private String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return escapeKeyword(value.trim());
    }

    /** LIKE 절의 특수 문자(%, _, \)를 이스케이프 */
    private String escapeKeyword(String keyword) {
        return keyword.replace("\\", "\\\\")
                      .replace("%", "\\%")
                      .replace("_", "\\_");
    }

    /** 요즘 뜨는 여행지 Top 10 — 최근 30일 PUBLIC 워크스페이스 기준, 데이터 부족 시 전체 기간 사용 */
    public List<TrendingDestinationResponse> getTrendingDestinations() {
        Pageable top10 = PageRequest.of(0, 10);
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        List<Object[]> rows = workspaceRepository.findTrendingDestinations(since, top10);
        if (rows.size() < 3) {
            rows = workspaceRepository.findTrendingDestinationsAllTime(top10);
        }

        List<TrendingDestinationResponse> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            result.add(new TrendingDestinationResponse(
                    i + 1,
                    (String) row[0],
                    (String) row[1],
                    (Long) row[2]
            ));
        }
        return result;
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }
}

package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.sns.dto.PublicWorkspaceResponse;
import com.sofly.core.domain.sns.entity.SnsPost;
import com.sofly.core.domain.sns.repository.SnsPostRepository;

import com.sofly.core.domain.sns.repository.UserFollowRepository;
import com.sofly.core.domain.sns.repository.WorkspaceCommentRepository;
import com.sofly.core.domain.sns.repository.WorkspaceLikeRepository;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private static final int CANDIDATE_LIMIT = 100;

    private final WorkspaceRepository workspaceRepository;
    private final UserFollowRepository userFollowRepository;
    private final WorkspaceLikeRepository workspaceLikeRepository;
    private final WorkspaceCommentRepository workspaceCommentRepository;
    private final SnsPostRepository snsPostRepository;

    public Page<PublicWorkspaceResponse> getFeed(Long userId, Pageable pageable) {
        Pageable bulk = PageRequest.of(0, CANDIDATE_LIMIT, Sort.by("createdAt").descending());

        List<Long> followingIds = userFollowRepository.findFollowingIdsByFollowerId(userId);
        List<Workspace> candidates = collectCandidates(followingIds, bulk);
        if (candidates.isEmpty()) return Page.empty(pageable);

        List<Long> ids = candidates.stream().map(Workspace::getId).toList();
        Map<Long, Long> likeCounts = toMap(workspaceLikeRepository.countByWorkspaceIds(ids));
        Map<Long, Long> commentCounts = toMap(workspaceCommentRepository.countByWorkspaceIds(ids));
        Set<Long> likedByUser = Set.copyOf(
                workspaceLikeRepository.findLikedWorkspaceIdsByUserId(userId, ids));

        // SNS 포스트: 워크스페이스 공개범위를 따르므로 visibility 별도 필터 불필요
        List<SnsPost> snsPosts = snsPostRepository.findByWorkspaceIdsWithImages(ids);
        Map<Long, SnsPost> snsPostMap = snsPosts.stream()
                .collect(Collectors.toMap(p -> p.getWorkspace().getId(), p -> p, (a, b) -> a));

        LocalDateTime recencyCutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(7);
        Random random = new Random();
        List<Workspace> sorted = candidates.stream()
                .sorted(Comparator.comparingDouble(
                        (Workspace w) -> scoredWithNoise(w, likeCounts, commentCounts, recencyCutoff, random)).reversed())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        if (start >= sorted.size()) return new PageImpl<>(List.of(), pageable, sorted.size());

        List<PublicWorkspaceResponse> responses = sorted.subList(start, end).stream()
                .map(w -> {
                    SnsPost snsPost = snsPostMap.get(w.getId());
                    Long snsPostId = snsPost != null ? snsPost.getId() : null;
                    String snsFirstImageUrl = (snsPost != null && !snsPost.getImages().isEmpty())
                            ? snsPost.getImages().get(0).getUrl() : null;
                    String snsPostContent = snsPost != null ? snsPost.getContent() : null;
                    return PublicWorkspaceResponse.of(
                            w,
                            likeCounts.getOrDefault(w.getId(), 0L),
                            commentCounts.getOrDefault(w.getId(), 0L),
                            likedByUser.contains(w.getId()),
                            snsPostId,
                            snsFirstImageUrl,
                            snsPostContent);
                })
                .toList();

        return new PageImpl<>(responses, pageable, sorted.size());
    }

    private List<Workspace> collectCandidates(List<Long> followingIds, Pageable bulk) {
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        List<Workspace> candidates = new ArrayList<>();

        if (!followingIds.isEmpty()) {
            workspaceRepository.findPublicByOwnerIds(followingIds, bulk)
                    .getContent().forEach(w -> { if (seen.add(w.getId())) candidates.add(w); });
            workspaceRepository.findFollowersOnlyByOwnerIds(followingIds, bulk)
                    .getContent().forEach(w -> { if (seen.add(w.getId())) candidates.add(w); });
        }

        workspaceRepository.findAllPublic(bulk)
                .getContent().forEach(w -> { if (seen.add(w.getId())) candidates.add(w); });

        return candidates;
    }

    private long score(Workspace w, Map<Long, Long> likeCounts,
                       Map<Long, Long> commentCounts, LocalDateTime cutoff) {
        long likeCount = likeCounts.getOrDefault(w.getId(), 0L);
        long commentCount = commentCounts.getOrDefault(w.getId(), 0L);
        long recencyBonus = (w.getCreatedAt() != null && w.getCreatedAt().isAfter(cutoff)) ? 10L : 0L;
        return likeCount * 3 + commentCount * 2 + recencyBonus;
    }

    private double scoredWithNoise(Workspace w, Map<Long, Long> likeCounts,
                                   Map<Long, Long> commentCounts, LocalDateTime cutoff, Random random) {
        double noise = 1.0 + (random.nextDouble() - 0.5) * 0.4;
        return score(w, likeCounts, commentCounts, cutoff) * noise;
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }
}

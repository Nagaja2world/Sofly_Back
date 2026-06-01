package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.sns.dto.PublicUserProfileResponse;
import com.sofly.core.domain.sns.dto.PublicWorkspaceResponse;
import com.sofly.core.domain.sns.repository.UserFollowRepository;
import com.sofly.core.domain.sns.repository.WorkspaceCommentRepository;
import com.sofly.core.domain.sns.repository.WorkspaceLikeRepository;
import com.sofly.core.domain.user.code.UserErrorCode;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.exception.UserException;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import com.sofly.core.global.response.PageResponse;
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
public class PublicProfileService {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceLikeRepository workspaceLikeRepository;
    private final WorkspaceCommentRepository workspaceCommentRepository;

    public PublicUserProfileResponse getProfile(Long targetUserId, Long viewerIdOrNull, Pageable pageable) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        long followerCount = userFollowRepository.countByFollowingId(targetUserId);
        long followingCount = userFollowRepository.countByFollowerId(targetUserId);
        boolean isFollowing = viewerIdOrNull != null &&
                userFollowRepository.existsByFollowerIdAndFollowingId(viewerIdOrNull, targetUserId);

        Page<Workspace> workspacePage = workspaceRepository.findPublicByUserId(targetUserId, pageable);
        PageResponse<PublicWorkspaceResponse> publicWorkspaces =
                PageResponse.from(toPublicPage(workspacePage, viewerIdOrNull, pageable));

        return new PublicUserProfileResponse(
                target.getId(),
                target.getNickname(),
                target.getProfileImageUrl(),
                followerCount,
                followingCount,
                isFollowing,
                publicWorkspaces
        );
    }

    private Page<PublicWorkspaceResponse> toPublicPage(Page<Workspace> workspacePage,
                                                        Long viewerIdOrNull,
                                                        Pageable pageable) {
        List<Workspace> workspaces = workspacePage.getContent();
        if (workspaces.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, workspacePage.getTotalElements());
        }

        List<Long> ids = workspaces.stream().map(Workspace::getId).toList();
        Map<Long, Long> likeCounts = toMap(workspaceLikeRepository.countByWorkspaceIds(ids));
        Map<Long, Long> commentCounts = toMap(workspaceCommentRepository.countByWorkspaceIds(ids));
        Set<Long> likedByViewer = viewerIdOrNull == null ? Set.of()
                : Set.copyOf(workspaceLikeRepository.findLikedWorkspaceIdsByUserId(viewerIdOrNull, ids));

        List<PublicWorkspaceResponse> responses = workspaces.stream().map(w -> {
            Boolean isLiked = viewerIdOrNull == null ? null : likedByViewer.contains(w.getId());
            return PublicWorkspaceResponse.of(
                    w,
                    likeCounts.getOrDefault(w.getId(), 0L),
                    commentCounts.getOrDefault(w.getId(), 0L),
                    isLiked,
                    null,
                    null);
        }).toList();

        return new PageImpl<>(responses, pageable, workspacePage.getTotalElements());
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }
}

package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.sns.entity.WorkspaceLike;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.sns.repository.UserFollowRepository;
import com.sofly.core.domain.sns.repository.WorkspaceLikeRepository;
import com.sofly.core.domain.user.code.UserErrorCode;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.exception.UserException;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.workspace.code.WorkspaceErrorCode;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.entity.WorkspaceVisibility;
import com.sofly.core.domain.workspace.exception.WorkspaceException;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.sofly.core.domain.sns.code.SnsErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final WorkspaceLikeRepository workspaceLikeRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserFollowRepository userFollowRepository;

    @Transactional
    public void like(Long userId, Long workspaceId) {
        if (workspaceLikeRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new SnsException(ALREADY_LIKED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        checkAccessibility(workspace, userId);
        workspaceLikeRepository.save(WorkspaceLike.builder()
                .user(user).workspace(workspace).build());
    }

    @Transactional
    public void unlike(Long userId, Long workspaceId) {
        if (!workspaceLikeRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new SnsException(LIKE_NOT_FOUND);
        }
        workspaceLikeRepository.deleteByWorkspaceIdAndUserId(workspaceId, userId);
    }

    public boolean isLiked(Long userId, Long workspaceId) {
        return workspaceLikeRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }

    public long getLikeCount(Long workspaceId) {
        return workspaceLikeRepository.countByWorkspaceId(workspaceId);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────

    /**
     * 워크스페이스 공개 범위에 따른 접근 권한 검증.
     * - PUBLIC: 모두 허용
     * - FOLLOWERS_ONLY: 본인(owner)이거나 팔로워인 경우만 허용
     * - PRIVATE: SNS 경로로는 접근 불가
     */
    private void checkAccessibility(Workspace workspace, Long viewerId) {
        WorkspaceVisibility visibility = workspace.getVisibility();
        if (visibility == WorkspaceVisibility.PUBLIC) {
            return;
        }
        if (visibility == WorkspaceVisibility.PRIVATE) {
            throw new SnsException(WORKSPACE_NOT_PUBLIC);
        }
        // FOLLOWERS_ONLY
        Long ownerId = workspace.getOwner().getId();
        if (!ownerId.equals(viewerId)
                && !userFollowRepository.existsByFollowerIdAndFollowingId(viewerId, ownerId)) {
            throw new SnsException(WORKSPACE_NOT_PUBLIC);
        }
    }
}

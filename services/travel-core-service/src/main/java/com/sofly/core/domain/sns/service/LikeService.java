package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.sns.entity.WorkspaceLike;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.sns.repository.WorkspaceLikeRepository;
import com.sofly.core.domain.user.code.UserErrorCode;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.exception.UserException;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.workspace.code.WorkspaceErrorCode;
import com.sofly.core.domain.workspace.entity.Workspace;
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

    @Transactional
    public void like(Long userId, Long workspaceId) {
        if (workspaceLikeRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new SnsException(ALREADY_LIKED);
        }
        UserWithWorkspace items = getUserAndWorkspaceOrThrow(userId, workspaceId);
        workspaceLikeRepository.save(WorkspaceLike.builder()
                .user(items.user()).workspace(items.workspace()).build());
    }

    @Transactional
    public void unlike(Long userId, Long workspaceId) {
        UserWithWorkspace items = getUserAndWorkspaceOrThrow(userId, workspaceId);
        workspaceLikeRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SnsException(LIKE_NOT_FOUND));
        workspaceLikeRepository.deleteByWorkspaceIdAndUserId(workspaceId, userId);
    }

    public boolean isLiked(Long userId, Long workspaceId) {
        return workspaceLikeRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }

    public long getLikeCount(Long workspaceId) {
        return workspaceLikeRepository.countByWorkspaceId(workspaceId);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────

    private record UserWithWorkspace(User user, Workspace workspace) {}

    private UserWithWorkspace getUserAndWorkspaceOrThrow(Long userId, Long workspaceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        return new UserWithWorkspace(user, workspace);
    }
}

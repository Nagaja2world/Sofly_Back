package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.sns.dto.CommentResponse;
import com.sofly.core.domain.sns.entity.WorkspaceComment;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.sns.repository.UserFollowRepository;
import com.sofly.core.domain.sns.repository.WorkspaceCommentRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.sofly.core.domain.sns.code.SnsErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final WorkspaceCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserFollowRepository userFollowRepository;

    public Page<CommentResponse> getComments(Long workspaceId, Long viewerIdOrNull, Pageable pageable) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        checkAccessibility(workspace, viewerIdOrNull);
        return commentRepository.findByWorkspaceIdWithAuthor(workspaceId, pageable)
                .map(CommentResponse::from);
    }

    @Transactional
    public CommentResponse createComment(Long userId, Long workspaceId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        checkAccessibility(workspace, userId);
        WorkspaceComment comment = WorkspaceComment.builder()
                .author(user).workspace(workspace).content(content).build();
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, String newContent) {
        WorkspaceComment comment = findCommentOrThrow(commentId);
        if (!comment.isAuthor(userId)) {
            throw new SnsException(COMMENT_FORBIDDEN);
        }
        comment.updateContent(newContent);
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        WorkspaceComment comment = findCommentOrThrow(commentId);
        if (!comment.isAuthor(userId)) {
            throw new SnsException(COMMENT_FORBIDDEN);
        }
        commentRepository.delete(comment);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────

    private WorkspaceComment findCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new SnsException(COMMENT_NOT_FOUND));
    }

    /**
     * 워크스페이스 공개 범위에 따른 접근 권한 검증.
     * - PUBLIC: 모두 허용
     * - FOLLOWERS_ONLY: 본인(owner)이거나 팔로워인 경우만 허용
     * - PRIVATE: SNS 경로로는 접근 불가
     */
    private void checkAccessibility(Workspace workspace, Long viewerIdOrNull) {
        WorkspaceVisibility visibility = workspace.getVisibility();
        if (visibility == WorkspaceVisibility.PUBLIC) {
            return;
        }
        if (visibility == WorkspaceVisibility.PRIVATE) {
            throw new SnsException(WORKSPACE_NOT_PUBLIC);
        }
        // FOLLOWERS_ONLY
        Long ownerId = workspace.getOwner().getId();
        if (viewerIdOrNull == null
                || (!ownerId.equals(viewerIdOrNull)
                    && !userFollowRepository.existsByFollowerIdAndFollowingId(viewerIdOrNull, ownerId))) {
            throw new SnsException(WORKSPACE_NOT_PUBLIC);
        }
    }
}

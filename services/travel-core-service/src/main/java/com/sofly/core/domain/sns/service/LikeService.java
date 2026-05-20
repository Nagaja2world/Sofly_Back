package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.sns.entity.WorkspaceLike;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.sns.repository.WorkspaceLikeRepository;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.sofly.core.domain.sns.code.SnsErrorCode.LIKE_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final WorkspaceLikeRepository workspaceLikeRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    //좋아요
    public void like(Long userId, Long workspaceId){
        UserWithWorkspace items = getUserAndWorkspaceOrThrow(userId, workspaceId);

        WorkspaceLike workspaceLike = WorkspaceLike.builder()
                .user(items.user)
                .workspace(items.workspace)
                .build();

        workspaceLikeRepository.save(workspaceLike);
    }

    //좋아요 취소
    public void unlike(Long userId, Long workspaceId){
        UserWithWorkspace items = getUserAndWorkspaceOrThrow(userId, workspaceId);

        Optional<WorkspaceLike> workspaceLike = workspaceLikeRepository.findByWorkspaceIdAndUserId(workspaceId, userId);

        if (workspaceLike.isEmpty()){
            throw new SnsException(LIKE_NOT_FOUND);
        } else {
            workspaceLikeRepository.deleteByWorkspaceIdAndUserId(workspaceId, userId);
        }
    }

    //좋아요 되어있는지
    public boolean islike(Long userId, Long workspaceId){
        return workspaceLikeRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }

    //좋아요 수
    public long getLikeCount(Long workspaceId){
        return workspaceLikeRepository.countByWorkspaceId(workspaceId);
    }

    // ----- 내부 함수 ----------------------------------------------------

    public record UserWithWorkspace(User user, Workspace workspace) {}

    private UserWithWorkspace getUserAndWorkspaceOrThrow(Long userId, Long workspaceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다. id=" + userId));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다. workspaceId=" + workspaceId));
        return new UserWithWorkspace(user, workspace);
    }
}

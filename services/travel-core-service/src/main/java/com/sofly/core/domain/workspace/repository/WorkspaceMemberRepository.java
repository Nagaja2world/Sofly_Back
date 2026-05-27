package com.sofly.core.domain.workspace.repository;

import com.sofly.core.domain.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    // 기존 findAllByWorkspaceId 삭제하고 아래로 교체
    @Query("SELECT wm FROM WorkspaceMember wm JOIN FETCH wm.user WHERE wm.workspace.id = :workspaceId")
    List<WorkspaceMember> findAllWithUserByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT wm FROM WorkspaceMember wm JOIN FETCH wm.user WHERE wm.workspace.id IN (SELECT wm2.workspace.id FROM WorkspaceMember wm2 WHERE wm2.user.id = :userId) AND wm.user.id != :userId")
    List<WorkspaceMember> findAllSharedWorkspaceMembers(@Param("userId") Long userId);
}

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

    List<WorkspaceMember> findAllByUserId(Long userId);

    List<WorkspaceMember> findAllByWorkspaceId(Long workspaceId); // 추가

    // wm.userId → wm.user.id 로 수정 (연관관계 필드)
    @Query("SELECT wm FROM WorkspaceMember wm JOIN FETCH wm.workspace WHERE wm.user.id = :userId")
    List<WorkspaceMember> findAllWithWorkspaceByUserId(@Param("userId") Long userId);
}

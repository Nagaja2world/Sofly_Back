package com.sofly.core.domain.sns.repository;

import com.sofly.core.domain.sns.entity.WorkspaceLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceLikeRepository extends JpaRepository<WorkspaceLike, Long> {

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    Optional<WorkspaceLike> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    long countByWorkspaceId(Long workspaceId);

    @Query("SELECT wl.workspace.id, COUNT(wl) FROM WorkspaceLike wl " +
           "WHERE wl.workspace.id IN :workspaceIds GROUP BY wl.workspace.id")
    List<Object[]> countByWorkspaceIds(@Param("workspaceIds") List<Long> workspaceIds);
}

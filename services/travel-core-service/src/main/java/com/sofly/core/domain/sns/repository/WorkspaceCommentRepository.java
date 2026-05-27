package com.sofly.core.domain.sns.repository;

import com.sofly.core.domain.sns.entity.WorkspaceComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkspaceCommentRepository extends JpaRepository<WorkspaceComment, Long> {

    @Query("SELECT wc FROM WorkspaceComment wc JOIN FETCH wc.author " +
           "WHERE wc.workspace.id = :workspaceId ORDER BY wc.createdAt ASC")
    Page<WorkspaceComment> findByWorkspaceIdWithAuthor(
            @Param("workspaceId") Long workspaceId, Pageable pageable);

    long countByWorkspaceId(Long workspaceId);

    @Query("SELECT wc.workspace.id, COUNT(wc) FROM WorkspaceComment wc " +
           "WHERE wc.workspace.id IN :workspaceIds GROUP BY wc.workspace.id")
    List<Object[]> countByWorkspaceIds(@Param("workspaceIds") List<Long> workspaceIds);
}

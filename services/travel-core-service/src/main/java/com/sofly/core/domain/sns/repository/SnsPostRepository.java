package com.sofly.core.domain.sns.repository;

import com.sofly.core.domain.sns.entity.SnsPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SnsPostRepository extends JpaRepository<SnsPost, Long> {

    boolean existsByWorkspaceId(Long workspaceId);

    Optional<SnsPost> findByWorkspaceId(Long workspaceId);

    /** 피드용: 워크스페이스 ID 목록에서 SNS 포스트 + 이미지 일괄 조회 */
    @Query("SELECT DISTINCT p FROM SnsPost p JOIN FETCH p.author LEFT JOIN FETCH p.images " +
           "WHERE p.workspace.id IN :workspaceIds")
    List<SnsPost> findByWorkspaceIdsWithImages(@Param("workspaceIds") List<Long> workspaceIds);
}

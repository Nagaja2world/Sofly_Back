package com.sofly.core.domain.sns.repository;

import com.sofly.core.domain.sns.entity.SnsPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SnsPostRepository extends JpaRepository<SnsPost, Long> {

    boolean existsByWorkspaceId(Long workspaceId);

    Optional<SnsPost> findByWorkspaceId(Long workspaceId);

    /** PUBLIC만 반환 (팔로워 없는 경우) */
    @Query("SELECT p FROM SnsPost p JOIN FETCH p.author JOIN FETCH p.workspace " +
           "WHERE p.visibility = :pub " +
           "ORDER BY p.createdAt DESC")
    Page<SnsPost> findPublicForFeed(
            @Param("pub") SnsPost.Visibility pub,
            Pageable pageable);

    /** PUBLIC + FOLLOWERS_ONLY(팔로잉 대상) 반환 */
    @Query("SELECT p FROM SnsPost p JOIN FETCH p.author JOIN FETCH p.workspace " +
           "WHERE p.visibility = :pub " +
           "OR (p.visibility = :followersOnly AND p.author.id IN :followingIds) " +
           "ORDER BY p.createdAt DESC")
    Page<SnsPost> findForFeed(
            @Param("pub") SnsPost.Visibility pub,
            @Param("followersOnly") SnsPost.Visibility followersOnly,
            @Param("followingIds") List<Long> followingIds,
            Pageable pageable);

    /** 피드용: 워크스페이스 ID 목록에서 SNS 포스트 + 첫번째 이미지 일괄 조회 */
    @Query("SELECT DISTINCT p FROM SnsPost p JOIN FETCH p.author LEFT JOIN FETCH p.images " +
           "WHERE p.workspace.id IN :workspaceIds AND p.visibility IN :visibilities")
    List<SnsPost> findByWorkspaceIdsWithImages(
            @Param("workspaceIds") List<Long> workspaceIds,
            @Param("visibilities") List<SnsPost.Visibility> visibilities);

    /** workspace별 이전 쿼리 유지 (삭제/수정 시 사용) */
    @Query("SELECT p FROM SnsPost p JOIN FETCH p.author JOIN FETCH p.workspace " +
           "WHERE p.workspace.id = :workspaceId " +
           "AND (p.visibility = :pub OR p.author.id = :userId) " +
           "ORDER BY p.createdAt DESC")
    Page<SnsPost> findByWorkspaceIdForUser(
            @Param("workspaceId") Long workspaceId,
            @Param("userId") Long userId,
            @Param("pub") SnsPost.Visibility pub,
            Pageable pageable);
}

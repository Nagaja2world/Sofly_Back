package com.sofly.core.domain.workspace.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.entity.WorkspaceVisibility;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @Query("SELECT w FROM Workspace w JOIN FETCH w.owner WHERE w.inviteCode = :inviteCode")
    Optional<Workspace> findWithOwnerByInviteCode(@Param("inviteCode") String inviteCode);

    @Query("SELECT w FROM Workspace w JOIN FETCH w.owner WHERE w.id = :workspaceId")
    Optional<Workspace> findWithOwnerById(@Param("workspaceId") Long workspaceId);

    @Query("SELECT DISTINCT wm.workspace FROM WorkspaceMember wm " +
           "JOIN FETCH wm.workspace.owner " +
           "WHERE wm.user.id = :userId")
    List<Workspace> findAllWithOwnerByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT wm.workspace FROM WorkspaceMember wm " +
           "JOIN FETCH wm.workspace.owner " +
           "WHERE wm.user.id = :userId AND wm.workspace.countryCode = :countryCode")
    List<Workspace> findAllByUserIdAndCountryCode(@Param("userId") Long userId,
                                                  @Param("countryCode") String countryCode);

    // ── SNS 공개 조회 ────────────────────────────────────────────────

    @Query("SELECT w FROM Workspace w JOIN FETCH w.owner " +
           "WHERE w.id = :workspaceId AND w.visibility = :visibility")
    Optional<Workspace> findByIdAndVisibility(@Param("workspaceId") Long workspaceId,
                                              @Param("visibility") WorkspaceVisibility visibility);

    @Query("SELECT w FROM Workspace w JOIN FETCH w.owner " +
           "WHERE w.owner.id IN :ownerIds AND w.visibility = 'PUBLIC'")
    Page<Workspace> findPublicByOwnerIds(@Param("ownerIds") List<Long> ownerIds, Pageable pageable);

    @Query("SELECT w FROM Workspace w JOIN FETCH w.owner " +
           "WHERE w.owner.id = :userId AND w.visibility = 'PUBLIC'")
    Page<Workspace> findPublicByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT w FROM Workspace w JOIN FETCH w.owner " +
           "WHERE w.visibility = 'PUBLIC' AND w.createdAt >= :since")
    Page<Workspace> findRecentPublic(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT w FROM Workspace w JOIN FETCH w.owner " +
           "WHERE w.visibility = 'PUBLIC' " +
           "AND (:countryCode IS NULL OR w.countryCode = :countryCode) " +
           "AND (:keyword IS NULL OR LOWER(w.destination) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(w.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Workspace> searchPublic(@Param("countryCode") String countryCode,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);
}

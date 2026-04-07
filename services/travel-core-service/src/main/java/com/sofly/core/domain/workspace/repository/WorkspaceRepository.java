package com.sofly.core.domain.workspace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sofly.core.domain.workspace.entity.Workspace;

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
}

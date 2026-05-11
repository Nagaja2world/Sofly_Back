package com.sofly.core.domain.workspace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sofly.core.domain.workspace.entity.WorkspaceInvitation;
import com.sofly.core.domain.workspace.entity.WorkspaceInvitation.InvitationStatus;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {

    boolean existsByWorkspaceIdAndInviteeIdAndStatus(Long workspaceId, Long inviteeId, InvitationStatus status);

    @Query("SELECT i FROM WorkspaceInvitation i JOIN FETCH i.workspace JOIN FETCH i.inviter WHERE i.invitee.id = :inviteeId AND i.status = :status")
    List<WorkspaceInvitation> findAllByInviteeIdAndStatus(@Param("inviteeId") Long inviteeId, @Param("status") InvitationStatus status);

    @Query("SELECT i FROM WorkspaceInvitation i JOIN FETCH i.workspace JOIN FETCH i.inviter WHERE i.id = :id AND i.invitee.id = :inviteeId")
    Optional<WorkspaceInvitation> findByIdAndInviteeId(@Param("id") Long id, @Param("inviteeId") Long inviteeId);
}

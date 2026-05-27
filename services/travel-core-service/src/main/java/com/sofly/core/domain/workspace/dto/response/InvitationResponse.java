package com.sofly.core.domain.workspace.dto.response;

import java.time.LocalDateTime;

import com.sofly.core.domain.workspace.entity.WorkspaceInvitation;
import com.sofly.core.domain.workspace.entity.WorkspaceInvitation.InvitationStatus;

public record InvitationResponse(
        Long invitationId,
        Long workspaceId,
        String workspaceTitle,
        String inviterNickname,
        String inviterEmail,
        InvitationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static InvitationResponse from(WorkspaceInvitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                invitation.getWorkspace().getTitle(),
                invitation.getInviter().getNickname(),
                invitation.getInviter().getEmail(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }
}

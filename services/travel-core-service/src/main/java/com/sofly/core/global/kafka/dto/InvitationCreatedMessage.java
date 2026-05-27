package com.sofly.core.global.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InvitationCreatedMessage {
    private Long invitationId;
    private Long workspaceId;
    private String workspaceTitle;
    private String inviterNickname;
    private Long inviteeId;
}

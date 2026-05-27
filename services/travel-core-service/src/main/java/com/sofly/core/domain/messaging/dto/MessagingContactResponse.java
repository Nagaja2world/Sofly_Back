package com.sofly.core.domain.messaging.dto;

import com.sofly.core.domain.workspace.entity.WorkspaceMember;

public record MessagingContactResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String email
) {
    public static MessagingContactResponse from(WorkspaceMember member) {
        return new MessagingContactResponse(
                member.getUser().getId(),
                member.getUser().getNickname(),
                member.getUser().getProfileImageUrl(),
                member.getUser().getEmail()
        );
    }
}

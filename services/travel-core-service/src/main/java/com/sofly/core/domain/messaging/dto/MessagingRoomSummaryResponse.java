package com.sofly.core.domain.messaging.dto;

import com.sofly.core.domain.messaging.entity.MessagingRoom;
import com.sofly.core.domain.messaging.enums.ChatRoomType;

public record MessagingRoomSummaryResponse(
        Long roomId,
        ChatRoomType type,
        String name,
        Long workspaceId
) {
    public static MessagingRoomSummaryResponse from(MessagingRoom room) {
        return new MessagingRoomSummaryResponse(
                room.getId(),
                room.getType(),
                room.getName(),
                room.getWorkspaceId()
        );
    }
}

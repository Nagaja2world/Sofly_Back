package com.sofly.core.domain.chat.dto;

import com.sofly.core.domain.chat.entity.ChatRoom;

public record ChatRoomSummaryResponse(
        Long roomId,
        String title,
        String lastMessage
) {
    public static ChatRoomSummaryResponse from(ChatRoom room) {
        return new ChatRoomSummaryResponse(room.getId(), room.getTitle(), room.getLastMessage());
    }
}

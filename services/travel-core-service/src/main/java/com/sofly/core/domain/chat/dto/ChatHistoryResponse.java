package com.sofly.core.domain.chat.dto;

import com.sofly.core.domain.chat.entity.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;

public record ChatHistoryResponse(
        Long roomId,
        List<MessageDto> messages
) {
    public record MessageDto(
            String content,
            ChatMessage.Role role,
            LocalDateTime createdAt
    ) {
        public static MessageDto from(ChatMessage message) {
            return new MessageDto(
                    message.getContent(),
                    message.getRole(),
                    message.getCreatedAt()
            );
        }
    }
}

// MessagingMessageResponse.java
package com.sofly.core.domain.messaging.dto;

import java.time.LocalDateTime;

import com.sofly.core.domain.messaging.document.MessagingMessage;
import com.sofly.core.domain.messaging.enums.ChatMessageType;

public record MessagingMessageResponse(
        String id,
        Long messagingRoomId,
        Long senderId,
        String senderNickname,
        String content,
        ChatMessageType type,
        LocalDateTime createdAt
) {
    public static MessagingMessageResponse from(MessagingMessage message) {
        return new MessagingMessageResponse(
                message.getId(),
                message.getMessagingRoomId(),
                message.getSenderId(),
                message.getSenderNickname(),
                message.getContent(),
                message.getType(),
                message.getCreatedAt()
        );
    }
}

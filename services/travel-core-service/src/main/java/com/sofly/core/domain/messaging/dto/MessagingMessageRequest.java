package com.sofly.core.domain.messaging.dto;

import com.sofly.core.domain.messaging.enums.ChatMessageType;

public record MessagingMessageRequest(
        Long senderId,
        String senderNickname,
        String content,
        ChatMessageType type
) {}

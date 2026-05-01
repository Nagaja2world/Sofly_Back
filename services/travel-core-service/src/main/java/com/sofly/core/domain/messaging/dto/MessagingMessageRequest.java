package com.sofly.core.domain.messaging.dto;

import com.sofly.core.domain.messaging.enums.ChatMessageType;

public record MessagingMessageRequest(
        String content,
        ChatMessageType type
) {}

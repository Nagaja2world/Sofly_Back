package com.sofly.core.domain.chat.dto;

import com.sofly.core.domain.chat.entity.ChatMessage;
import java.time.LocalDateTime;

public record ChatResponse(
        Long roomId,
        String message,
        ChatMessage.Role role,
        LocalDateTime createdAt
) {}

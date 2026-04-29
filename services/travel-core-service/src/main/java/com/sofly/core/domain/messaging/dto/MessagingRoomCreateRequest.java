package com.sofly.core.domain.messaging.dto;

import java.util.List;

import com.sofly.core.domain.messaging.enums.ChatRoomType;

public record MessagingRoomCreateRequest(
        ChatRoomType type,
        String name,
        Long workspaceId,
        List<Long> memberIds  // 초대할 유저 ID 목록
) {}

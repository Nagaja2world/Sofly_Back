package com.sofly.core.domain.chat.dto;

import jakarta.validation.constraints.NotNull;

public record ChatRoomCreateRequest(
        @NotNull Long workspaceId,
        String title
) {}

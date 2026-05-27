package com.sofly.core.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRoomUpdateRequest(
        @NotBlank String title
) {}

package com.sofly.core.domain.workspace.dto.request;

import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotNull Long userId
) {}

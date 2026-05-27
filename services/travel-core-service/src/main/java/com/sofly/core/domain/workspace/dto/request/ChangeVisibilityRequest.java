package com.sofly.core.domain.workspace.dto.request;

import com.sofly.core.domain.workspace.entity.WorkspaceVisibility;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChangeVisibilityRequest {

    @NotNull
    private WorkspaceVisibility visibility;
}

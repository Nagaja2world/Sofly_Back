package com.sofly.core.domain.workspace.dto.request;

import com.sofly.core.domain.workspace.entity.WorkspaceMember.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateMemberRoleRequest {

    @NotNull
    private MemberRole role;
}

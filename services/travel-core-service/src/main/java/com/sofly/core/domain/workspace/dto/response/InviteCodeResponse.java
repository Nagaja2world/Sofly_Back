package com.sofly.core.domain.workspace.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InviteCodeResponse {

    private String inviteCode;
    private String inviteUrl;

    public static InviteCodeResponse of(String inviteCode, String baseUrl) {
        return InviteCodeResponse.builder()
                .inviteCode(inviteCode)
                .inviteUrl(baseUrl + "/workspaces/join/" + inviteCode)
                .build();
    }
}

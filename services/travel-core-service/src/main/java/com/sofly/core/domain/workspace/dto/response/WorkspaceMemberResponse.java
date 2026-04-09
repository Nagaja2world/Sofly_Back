package com.sofly.core.domain.workspace.dto.response;

import com.sofly.core.domain.workspace.entity.WorkspaceMember;
import com.sofly.core.domain.workspace.entity.WorkspaceMember.MemberRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkspaceMemberResponse {

    private Long memberId;
    private Long userId;
    private String nickname;       // name → nickname
    private String userEmail;
    private MemberRole role;

    public static WorkspaceMemberResponse from(WorkspaceMember member) {
        return WorkspaceMemberResponse.builder()
                .memberId(member.getId())
                .userId(member.getUser().getId())
                .nickname(member.getUser().getNickname()) // getNickname()으로 수정
                .userEmail(member.getUser().getEmail())
                .role(member.getRole())
                .build();
    }
}

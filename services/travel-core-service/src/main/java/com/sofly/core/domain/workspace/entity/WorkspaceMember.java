
package com.sofly.core.domain.workspace.entity;

import com.sofly.core.domain.user.entity.User;
import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workspace_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class WorkspaceMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberRole role = MemberRole.VIEWER;

    public enum MemberRole {
        OWNER,   // 소유자 (Workspace.owner 와 동기화)
        EDITOR,  // 일정 수정 가능
        VIEWER   // 조회만
    }

    public void updateRole(MemberRole role) {
        this.role = role;
    }

    public static WorkspaceMember ofOwner(Workspace workspace, User user) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(MemberRole.OWNER)
                .build();
    }

    public static WorkspaceMember ofViewer(Workspace workspace, User user) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(MemberRole.VIEWER)
                .build();
    }
}

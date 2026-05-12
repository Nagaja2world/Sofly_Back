package com.sofly.core.domain.workspace.entity;

import com.sofly.core.domain.user.entity.User;
import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workspaces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Workspace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;               // 여행 제목

    @Column(nullable = false)
    private String destination;         // 목적지 (도시/국가명)

    private String countryCode;         // ISO 3166-1 alpha-2 (정복도용, 예: JP)

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private Integer headcount;          // 동행 인원

    private String coverImageUrl;       // 대표 이미지

    private String inviteCode;          // 초대 링크 코드 (UUID)

    // 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // 멤버 목록
    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkspaceMember> members = new ArrayList<>();

    // ── 비즈니스 메서드 ──────────────────────────────────────

    public void addMember(WorkspaceMember member) {
        this.members.add(member);
        member.setWorkspace(this);
    }
    public void update(String title, String destination, LocalDate startDate,
                       LocalDate endDate, Integer headcount, String coverImageUrl) {
        this.title = title;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.headcount = headcount;
        this.coverImageUrl = coverImageUrl;
    }

    public void generateInviteCode(String code) {
        this.inviteCode = code;
    }

    public boolean isOwner(Long userId) {
        return this.owner.getId().equals(userId);
    }

    public void updateCoverImage(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }
}

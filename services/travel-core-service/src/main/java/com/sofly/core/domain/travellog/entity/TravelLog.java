package com.sofly.core.domain.travellog.entity;

import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "travel_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class TravelLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;             // Markdown

    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    // ── 비즈니스 메서드 ──────────────────────────────────────

    public void update(String title, String content, String coverImageUrl, Visibility visibility) {
        this.title = title;
        this.content = content;
        this.coverImageUrl = coverImageUrl;
        this.visibility = visibility;
    }

    public boolean isAuthor(Long userId) {
        return this.author.getId().equals(userId);
    }

    public enum Visibility {
        PRIVATE,    // 비공개
        MEMBERS,    // 멤버 공개
        PUBLIC      // 전체 공개
    }
}

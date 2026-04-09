package com.sofly.core.domain.travellog.entity;

import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    private Integer day;

    private LocalDate travelDate;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;             // Markdown

    @Enumerated(EnumType.STRING)
    private Weather weather;

    @ElementCollection
    @CollectionTable(name = "travel_log_photos", joinColumns = @JoinColumn(name = "travel_log_id"))
    @Column(name = "url")
    @Builder.Default
    private List<String> photoUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // ── 비즈니스 메서드 ──────────────────────────────────────

    public void update(String title, String content, Visibility visibility) {
        this.title = title;
        this.content = content;
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

    public enum Weather {
        SUNNY,
        CLOUDY,
        RAINY,
        SNOWY
    }
}

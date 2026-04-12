package com.sofly.core.domain.travellog.entity;

import com.sofly.core.domain.album.entity.Photo;
import com.sofly.core.domain.travellog.dto.TravellogUpdateRequest;
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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "travel_log_photos",
            joinColumns = @JoinColumn(name = "travel_log_id"),
            inverseJoinColumns = @JoinColumn(name = "photo_id")
    )
    @Builder.Default
    private List<Photo> photos = new ArrayList<>();

//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    @Builder.Default
//    private Visibility visibility = Visibility.PRIVATE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // ── 비즈니스 메서드 ──────────────────────────────────────

    public void update(TravellogUpdateRequest request) {
        if (request.day() != null) this.day = request.day();
        if (request.travelDate() != null) this.travelDate = request.travelDate();
        if (request.title() != null) this.title = request.title();
        if (request.content() != null) this.content = request.content();
        if (request.weather() != null) this.weather = request.weather();
    }

    public boolean isAuthor(Long userId) {
        return this.author.getId().equals(userId);
    }

    public void addPhoto(Photo photo) {
        this.photos.add(photo);
    }

    public void removePhoto(Photo photo) {
        this.photos.remove(photo);
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

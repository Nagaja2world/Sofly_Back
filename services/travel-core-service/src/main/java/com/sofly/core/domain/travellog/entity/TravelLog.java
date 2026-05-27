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

    private String mainTitle;

    private LocalDate travelDate;

    private String title;

    @Column(columnDefinition = "TEXT")
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

    // TODO: 공개 범위 기능 구현 예정 (PRIVATE / MEMBERS / PUBLIC)
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    @Builder.Default
//    private Visibility visibility = Visibility.PRIVATE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // ── 비즈니스 메서드 ──────────────────────────────────────

    public void update(TravellogUpdateRequest request) {
        if (request.mainTitle() != null) this.mainTitle = request.mainTitle();
        if (request.travelDate() != null) this.travelDate = request.travelDate();
        if (request.title() != null) this.title = request.title();
        if (request.content() != null) this.content = request.content();
        if (request.weather() != null) this.weather = request.weather();
    }

    // TODO: 수정/삭제 권한 정책 결정 필요
    //  - 작성자 본인만 가능: isAuthor(userId) 체크를 서비스에 추가
    //  - 워크스페이스 EDITOR 이상이면 모두 가능: isAuthor 메서드 제거
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

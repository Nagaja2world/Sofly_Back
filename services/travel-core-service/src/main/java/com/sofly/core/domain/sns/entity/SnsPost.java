package com.sofly.core.domain.sns.entity;

import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sns_posts",
       uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "workspace_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SnsPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @OneToMany(mappedBy = "snsPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<SnsPostImage> images = new ArrayList<>();

    public enum Visibility {
        PUBLIC,           // 전체공개
        FOLLOWERS_ONLY,   // 팔로워만
        PRIVATE           // 나만 보기
    }

    public void update(String content, Visibility visibility) {
        if (content != null) this.content = content;
        if (visibility != null) this.visibility = visibility;
    }

    public void clearImages() {
        this.images.clear();
    }

    public boolean isAuthor(Long userId) {
        return this.author.getId().equals(userId);
    }

    public void addImage(SnsPostImage image) {
        this.images.add(image);
    }

    public void removeImage(SnsPostImage image) {
        this.images.remove(image);
    }
}

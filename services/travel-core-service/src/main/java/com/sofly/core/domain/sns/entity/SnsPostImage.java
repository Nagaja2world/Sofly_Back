package com.sofly.core.domain.sns.entity;

import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "sns_post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SnsPostImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sns_post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SnsPost snsPost;

    @Column(nullable = false)
    private String s3Key;

    @Column(nullable = false)
    private String url;

    private int orderIndex;

    public static SnsPostImage of(SnsPost post, String s3Key, String url, int orderIndex) {
        return SnsPostImage.builder()
                .snsPost(post)
                .s3Key(s3Key)
                .url(url)
                .orderIndex(orderIndex)
                .build();
    }

    public void updateOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}

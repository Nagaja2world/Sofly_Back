package com.sofly.core.domain.album.entity;

import com.sofly.core.domain.user.entity.User;
import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Photo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Album album;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private String s3Key;           // S3 객체 키 (삭제 시 사용)

    @Column(nullable = false)
    private String url;             // S3 접근 URL (또는 CloudFront URL)

    // EXIF 정보 (선택)
    private LocalDate takenAt;
    private Double latitude;
    private Double longitude;

    private Integer matchedDay;     // EXIF 기반 매칭된 여행 일차

    public static Photo of(Album album, User uploadedBy, String s3Key, String url,
                           LocalDate takenAt, Double latitude, Double longitude) {
        return Photo.builder()
                .album(album)
                .uploadedBy(uploadedBy)
                .s3Key(s3Key)
                .url(url)
                .takenAt(takenAt)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}

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
    private Album album;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private String thumbnailUrl;        // 썸네일 URL

    @Column(nullable = false)
    private String originalUrl;         // 원본 URL (Drive 링크)

    private String driveFileId;         // Google Drive 파일 ID

    // EXIF 정보 (선택)
    private LocalDate takenAt;          // 촬영 날짜
    private Double latitude;            // 촬영 위치
    private Double longitude;

    private Integer matchedDay;         // EXIF 기반 매칭된 여행 일차
}

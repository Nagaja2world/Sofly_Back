package com.sofly.core.domain.schedule.entity;

import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "schedule_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ScheduleItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(nullable = false)
    private Integer day;                // 여행 일차 (1부터 시작)

    @Column(nullable = false)
    private Integer orderIndex;         // 일차 내 순서 (D&D 로 변경)

    private LocalTime visitTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;          // ACCOMMODATION, RESTAURANT, CAFE, ATTRACTION, TRANSPORT

    @Column(nullable = false)
    private String name;                // 장소/활동 이름

    private String address;

    private Double latitude;

    private Double longitude;

    @Column
    private String placeId; // Google places ID (nullable)

    @Column
    private String photoReference; // 대표 사진 resource name (nullable)

    @Column(columnDefinition = "TEXT")
    private String memo;

    private String deepLinkUrl;         // 예약 딥링크 (숙소/교통)

    private Double estimatedCost;       // 예상 비용 (원)

    private Integer deepLinkClickCount; // 딥링크 클릭 통계

    // ── 비즈니스 메서드 ──────────────────────────────────────

    public void update(LocalTime visitTime, String memo, Category category, String address, Double estimatedCost, String name) {
        this.visitTime = visitTime;
        this.memo = memo;
        this.category = category;
        this.address = address;
        this.estimatedCost = estimatedCost;
        this.name = name;
    }

    public void updatePlace(String placeId, String photoReference, Double latitude, Double longitude) {
        if (placeId != null) this.placeId = placeId;
        if (photoReference != null) this.photoReference = photoReference;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
    }

    public void updateOrder(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public void incrementDeepLinkClick() {
        this.deepLinkClickCount = (this.deepLinkClickCount == null ? 0 : this.deepLinkClickCount) + 1;
    }

    public enum Category {
        ACCOMMODATION,  // 숙소
        RESTAURANT,     // 맛집
        CAFE,           // 카페
        ATTRACTION,     // 관광지
        TRANSPORT       // 교통
    }

    public void moveToDay(Integer day) {
        this.day = day;
    }
}

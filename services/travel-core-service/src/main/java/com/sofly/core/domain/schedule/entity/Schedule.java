
package com.sofly.core.domain.schedule.entity;

import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    private String title;               // 일정 버전 제목 (예: "1차 생성", "수정본")

    @Column(nullable = false)
    private Integer version;            // 버전 번호

    @Column(columnDefinition = "TEXT")
    private String aiChatSessionId;     // AI 채팅 세션 ID

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("day ASC, orderIndex ASC")
    @Builder.Default
    private List<ScheduleItem> items = new ArrayList<>();

    // ── 비즈니스 메서드 ──────────────────────────────────────

    public void addItem(ScheduleItem item) {
        this.items.add(item);
        // item.setSchedule(this); // ScheduleItem에 setSchedule(Schedule) 추가 필요
    }

    public void updateTitle(String title) {
        this.title = title;
    }
}

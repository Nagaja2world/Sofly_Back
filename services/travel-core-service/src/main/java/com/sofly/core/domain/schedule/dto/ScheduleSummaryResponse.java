package com.sofly.core.domain.schedule.dto;

import com.sofly.core.domain.schedule.entity.Schedule;

import java.time.LocalDateTime;

// 일정 목록 응답 (아이템 미포함, 버전 리스트용)
public record ScheduleSummaryResponse(

        Long id,
        String title,
        Integer version,
        int itemCount,
        LocalDateTime createdAt
) {
    public static ScheduleSummaryResponse from(Schedule schedule) {
        return new ScheduleSummaryResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getVersion(),
                schedule.getItems().size(),
                schedule.getCreatedAt()
        );
    }
}
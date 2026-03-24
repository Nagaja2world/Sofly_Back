package com.sofly.core.domain.schedule.dto;

import com.sofly.core.domain.schedule.entity.Schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 일정 단건 응답 (아이템 포함, day별로 그룹핑)
public record ScheduleResponse(

        Long id,
        Long workspaceId,
        String title,
        Integer version,
        String aiChatSessionId,
        Map<Integer, List<ScheduleItemResponse>> itemsByDay,  // { 1: [...], 2: [...] }
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScheduleResponse from(Schedule schedule) {
        Map<Integer, List<ScheduleItemResponse>> itemsByDay = schedule.getItems().stream()
                .map(ScheduleItemResponse::from)
                .collect(Collectors.groupingBy(ScheduleItemResponse::day));

        return new ScheduleResponse(
                schedule.getId(),
                schedule.getWorkspace().getId(),
                schedule.getTitle(),
                schedule.getVersion(),
                schedule.getAiChatSessionId(),
                itemsByDay,
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
package com.sofly.core.domain.sns.dto;

import com.sofly.core.domain.schedule.entity.Schedule;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ScheduleSummary(
        Integer version,
        Map<Integer, List<ScheduleItemSummary>> itemsByDay
) {
    public static ScheduleSummary from(Schedule schedule) {
        Map<Integer, List<ScheduleItemSummary>> itemsByDay = schedule.getItems().stream()
                .map(ScheduleItemSummary::from)
                .collect(Collectors.groupingBy(ScheduleItemSummary::day));
        return new ScheduleSummary(schedule.getVersion(), itemsByDay);
    }
}

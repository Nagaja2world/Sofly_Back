package com.sofly.core.domain.sns.dto;

import com.sofly.core.domain.schedule.entity.ScheduleItem;

import java.time.LocalTime;

public record ScheduleItemSummary(
        Integer day,
        Integer orderIndex,
        ScheduleItem.Category category,
        String name,
        String address,
        LocalTime visitTime,
        String memo
) {
    public static ScheduleItemSummary from(ScheduleItem item) {
        return new ScheduleItemSummary(
                item.getDay(),
                item.getOrderIndex(),
                item.getCategory(),
                item.getName(),
                item.getAddress(),
                item.getVisitTime(),
                item.getMemo()
        );
    }
}

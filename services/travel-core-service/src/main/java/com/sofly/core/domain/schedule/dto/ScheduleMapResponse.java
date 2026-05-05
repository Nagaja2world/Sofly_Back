package com.sofly.core.domain.schedule.dto;

import com.sofly.core.domain.schedule.entity.ScheduleItem;

import java.time.LocalTime;
import java.util.List;

public record ScheduleMapResponse(
        List<DayGroup> days
) {
    public record DayGroup(
            Integer day,
            List<MapPin> pins
    ){}

    public record MapPin(
      Long scheduleItemId,
      String name,
      ScheduleItem.Category category,
      Double latitude,
      Double longitude,
      String placeId,
      String photoReference,
      LocalTime visitTime
    ){}
}

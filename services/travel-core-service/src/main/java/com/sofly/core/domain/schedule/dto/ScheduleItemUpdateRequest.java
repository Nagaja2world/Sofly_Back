package com.sofly.core.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sofly.core.domain.schedule.entity.ScheduleItem.Category;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

// 아이템 단건 수정 (visitTime, memo, category, address, estimatedCost, name)
public record ScheduleItemUpdateRequest(

        @JsonFormat(pattern = "HH:mm")
        LocalTime visitTime,

        String memo,

        @NotNull
        Category category,

        String address,

        Double estimatedCost,

        String name,

        String placeId,

        String photoReference,

        Double latitude,

        Double longitude
) {}

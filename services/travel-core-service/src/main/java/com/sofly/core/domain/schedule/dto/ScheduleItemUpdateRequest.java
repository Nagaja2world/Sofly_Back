package com.sofly.core.domain.schedule.dto;

import com.sofly.core.domain.schedule.entity.ScheduleItem.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 아이템 단건 수정 (visitTime, memo, category)
public record ScheduleItemUpdateRequest(

        String visitTime,

        String memo,

        @NotNull
        Category category
) {}
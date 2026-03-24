package com.sofly.core.domain.schedule.dto;

import com.sofly.core.domain.schedule.entity.ScheduleItem.Category;
import jakarta.validation.constraints.*;

// 일정 아이템 생성 (ScheduleCreateRequest 내부에서 사용)
public record ScheduleItemCreateRequest(

        @NotNull @Min(1)
        Integer day,

        @NotNull @Min(0)
        Integer orderIndex,

        String visitTime,           // "HH:mm"

        @NotNull
        Category category,

        @NotBlank
        String name,

        String address,

        Double latitude,
        Double longitude,

        String memo,
        String deepLinkUrl,
        Integer estimatedCost
) {}
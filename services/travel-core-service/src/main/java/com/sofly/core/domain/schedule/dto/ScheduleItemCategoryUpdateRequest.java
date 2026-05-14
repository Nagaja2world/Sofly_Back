package com.sofly.core.domain.schedule.dto;

import com.sofly.core.domain.schedule.entity.ScheduleItem.Category;
import jakarta.validation.constraints.NotNull;

public record ScheduleItemCategoryUpdateRequest(
        @NotNull
        Category category
) {}

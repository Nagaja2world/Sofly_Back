package com.sofly.core.domain.schedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScheduleItemMoveRequest(
        @NotNull @Min(1) Integer targetDay,
        @NotNull @Min(0) Integer targetOrderIndex
) {}

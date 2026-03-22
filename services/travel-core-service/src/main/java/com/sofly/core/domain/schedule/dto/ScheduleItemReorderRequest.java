package com.sofly.core.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.List;

// D&D 순서 변경 - 여러 아이템을 한 번에 받아서 처리
public record ScheduleItemReorderRequest(

        @NotNull
        List<ItemOrder> orders
) {
    public record ItemOrder(
            @NotNull Long itemId,
            @NotNull @Min(1) Integer day,
            @NotNull @Min(0) Integer orderIndex
    ) {}
}
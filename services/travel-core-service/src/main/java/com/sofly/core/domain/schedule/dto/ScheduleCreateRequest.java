package com.sofly.core.domain.schedule.dto;

import com.sofly.core.domain.schedule.entity.ScheduleItem.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

// 일정 최초 생성 (AI가 만들어준 결과를 저장)
public record ScheduleCreateRequest(

        @NotNull
        Long workspaceId,

        String title,

        String aiChatSessionId,

        @NotNull @Valid
        List<ScheduleItemCreateRequest> items
) {}
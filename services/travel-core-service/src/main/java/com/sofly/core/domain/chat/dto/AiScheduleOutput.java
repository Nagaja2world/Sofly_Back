package com.sofly.core.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sofly.core.domain.schedule.entity.ScheduleItem.Category;

import java.util.List;

/**
 * AI가 3단계(확정)에서 출력하는 JSON 구조를 파싱하기 위한 중간 DTO.
 * SystemPrompts.TRAVEL_PLANNER의 JSON 형식과 1:1 대응.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiScheduleOutput(List<AiDay> days) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiDay(int day, List<AiItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiItem(
            int orderIndex,
            String name,
            Category category,
            String visitTime,       // "HH:mm" 형식 문자열, nullable
            String address,
            Double latitude,
            Double longitude,
            String placeId,
            String photoReference,
            Double estimatedCost,
            String memo,
            String deepLinkUrl
    ) {}
}

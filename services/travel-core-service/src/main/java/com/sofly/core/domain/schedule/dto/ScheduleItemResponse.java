package com.sofly.core.domain.schedule.dto;

import com.sofly.core.domain.schedule.entity.ScheduleItem;
import com.sofly.core.domain.schedule.entity.ScheduleItem.Category;

import java.time.LocalDateTime;

// 아이템 단건 응답
public record ScheduleItemResponse(

        Long id,
        Integer day,
        Integer orderIndex,
        String visitTime,
        Category category,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String memo,
        String deepLinkUrl,
        Integer estimatedCost,
        Integer deepLinkClickCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScheduleItemResponse from(ScheduleItem item) {
        return new ScheduleItemResponse(
                item.getId(),
                item.getDay(),
                item.getOrderIndex(),
                item.getVisitTime(),
                item.getCategory(),
                item.getName(),
                item.getAddress(),
                item.getLatitude(),
                item.getLongitude(),
                item.getMemo(),
                item.getDeepLinkUrl(),
                item.getEstimatedCost(),
                item.getDeepLinkClickCount(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
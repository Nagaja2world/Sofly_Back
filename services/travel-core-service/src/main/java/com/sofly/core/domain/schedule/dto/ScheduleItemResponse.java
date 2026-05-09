package com.sofly.core.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sofly.core.domain.schedule.entity.ScheduleItem;
import com.sofly.core.domain.schedule.entity.ScheduleItem.Category;

import java.time.LocalDateTime;
import java.time.LocalTime;

// 아이템 단건 응답
public record ScheduleItemResponse(

        Long id,
        Integer day,
        Integer orderIndex,
        @JsonFormat(pattern = "HH:mm") LocalTime visitTime,
        Category category,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String placeId,
        String photoReference,
        String memo,
        String deepLinkUrl,
        Double estimatedCost,
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
                item.getPlaceId(),
                item.getPhotoReference(),
                item.getMemo(),
                item.getDeepLinkUrl(),
                item.getEstimatedCost(),
                item.getDeepLinkClickCount(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}

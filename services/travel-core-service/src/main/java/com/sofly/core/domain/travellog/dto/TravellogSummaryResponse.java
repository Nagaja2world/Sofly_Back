package com.sofly.core.domain.travellog.dto;

import com.sofly.core.domain.travellog.entity.TravelLog;
import com.sofly.core.domain.travellog.entity.TravelLog.Weather;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TravellogSummaryResponse(
        Long id,
        Integer day,
        LocalDate travelDate,
        String title,
        Weather weather,
        int photoCount,
        LocalDateTime createdAt
) {
    public static TravellogSummaryResponse from(TravelLog log) {
        return new TravellogSummaryResponse(
                log.getId(),
                log.getDay(),
                log.getTravelDate(),
                log.getTitle(),
                log.getWeather(),
                log.getPhotos().size(),
                log.getCreatedAt()
        );
    }
}

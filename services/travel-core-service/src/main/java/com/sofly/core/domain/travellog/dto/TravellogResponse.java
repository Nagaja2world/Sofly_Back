package com.sofly.core.domain.travellog.dto;

import com.sofly.core.domain.album.dto.PhotoResponse;
import com.sofly.core.domain.travellog.entity.TravelLog;
import com.sofly.core.domain.travellog.entity.TravelLog.Weather;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TravellogResponse(
        Long id,
        Integer day,
        LocalDate travelDate,
        String title,
        String content,
        Weather weather,
        Long workspaceId,
        Long authorId,
        String authorNickname,
        List<PhotoResponse> photos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TravellogResponse from(TravelLog log) {
        List<PhotoResponse> photoResponses = log.getPhotos().stream()
                .map(PhotoResponse::from)
                .toList();

        return new TravellogResponse(
                log.getId(),
                log.getDay(),
                log.getTravelDate(),
                log.getTitle(),
                log.getContent(),
                log.getWeather(),
                log.getWorkspace().getId(),
                log.getAuthor().getId(),
                log.getAuthor().getNickname(),
                photoResponses,
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}

package com.sofly.core.domain.album.dto;

import com.sofly.core.domain.album.entity.Photo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PhotoResponse(
        Long id,
        String url,
        Long uploadedById,
        String uploadedByNickname,
        LocalDate takenAt,
        Double latitude,
        Double longitude,
        Integer matchedDay,
        LocalDateTime createdAt
) {
    public static PhotoResponse from(Photo photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getUrl(),
                photo.getUploadedBy().getId(),
                photo.getUploadedBy().getNickname(),
                photo.getTakenAt(),
                photo.getLatitude(),
                photo.getLongitude(),
                photo.getMatchedDay(),
                photo.getCreatedAt()
        );
    }
}

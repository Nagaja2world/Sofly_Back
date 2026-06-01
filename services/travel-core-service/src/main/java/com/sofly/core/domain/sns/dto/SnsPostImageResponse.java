package com.sofly.core.domain.sns.dto;

import com.sofly.core.domain.sns.entity.SnsPostImage;

public record SnsPostImageResponse(
        Long id,
        String url,
        int orderIndex
) {
    public static SnsPostImageResponse from(SnsPostImage image) {
        return new SnsPostImageResponse(image.getId(), image.getUrl(), image.getOrderIndex());
    }
}

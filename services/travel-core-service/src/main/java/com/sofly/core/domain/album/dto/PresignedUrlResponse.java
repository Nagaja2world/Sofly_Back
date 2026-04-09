package com.sofly.core.domain.album.dto;

public record PresignedUrlResponse(
        String presignedUrl,
        String s3Key
) {}

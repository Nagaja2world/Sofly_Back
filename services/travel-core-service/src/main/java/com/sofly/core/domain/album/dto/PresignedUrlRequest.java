package com.sofly.core.domain.album.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedUrlRequest(
        @NotBlank String fileName,
        @NotBlank String contentType
) {}

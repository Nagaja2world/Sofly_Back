package com.sofly.core.domain.album.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record PhotoSaveRequest(
        @NotBlank String s3Key,
        LocalDate takenAt,
        Double latitude,
        Double longitude
) {}

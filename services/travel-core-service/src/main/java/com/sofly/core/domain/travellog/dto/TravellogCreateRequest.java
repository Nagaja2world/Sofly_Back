package com.sofly.core.domain.travellog.dto;

import com.sofly.core.domain.travellog.entity.TravelLog.Weather;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record TravellogCreateRequest(
        Integer day,
        LocalDate travelDate,
        @NotBlank String title,
        @NotBlank String content,
        Weather weather
) {}

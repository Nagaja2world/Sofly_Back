package com.sofly.core.domain.travellog.dto;

import com.sofly.core.domain.travellog.entity.TravelLog.Weather;

import java.time.LocalDate;

public record TravellogUpdateRequest(
        Integer day,
        LocalDate travelDate,
        String title,
        String content,
        Weather weather
) {}

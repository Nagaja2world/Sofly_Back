package com.sofly.core.domain.travellog.dto;

import com.sofly.core.domain.travellog.entity.TravelLog.Weather;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TravellogUpdateRequest(
        String mainTitle,
        LocalDate travelDate,
        @Size(min = 1, message = "제목은 빈 값일 수 없습니다") String title,
        @Size(min = 1, message = "내용은 빈 값일 수 없습니다") String content,
        Weather weather
) {}

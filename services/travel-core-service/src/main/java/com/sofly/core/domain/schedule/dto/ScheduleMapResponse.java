package com.sofly.core.domain.schedule.dto;

import com.sofly.core.domain.schedule.entity.ScheduleItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.List;

@Schema(description = "일정 지도 핀 응답")
public record ScheduleMapResponse(
        @Schema(description = "일차별 핀 그룹 목록") List<DayGroup> days
) {
    @Schema(description = "일차별 핀 그룹")
    public record DayGroup(
            @Schema(description = "일차 (1부터 시작)", example = "1") Integer day,
            @Schema(description = "해당 일차의 지도 핀 목록") List<MapPin> pins
    ){}

    @Schema(description = "지도 핀 (좌표가 있는 일정 아이템)")
    public record MapPin(
            @Schema(description = "일정 아이템 ID", example = "42") Long scheduleItemId,
            @Schema(description = "장소명", example = "경복궁") String name,
            @Schema(description = "카테고리") ScheduleItem.Category category,
            @Schema(description = "위도", example = "37.5796") Double latitude,
            @Schema(description = "경도", example = "126.9770") Double longitude,
            @Schema(description = "Google Places ID", example = "ChIJN1t_tDeuEmsRUsoyG83frY4") String placeId,
            @Schema(description = "Google Places 사진 레퍼런스") String photoReference,
            @Schema(description = "방문 예정 시각", example = "10:00:00") LocalTime visitTime
    ){}
}

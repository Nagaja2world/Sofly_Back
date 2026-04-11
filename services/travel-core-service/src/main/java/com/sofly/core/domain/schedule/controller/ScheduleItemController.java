package com.sofly.core.domain.schedule.controller;

import com.sofly.core.domain.schedule.dto.*;
import com.sofly.core.domain.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Schedule Item", description = "일정 아이템 추가·수정·이동·삭제 API")
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleItemController {

    private final ScheduleService scheduleService;

    @Operation(summary = "일정 아이템 추가")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추가 성공"),
            @ApiResponse(responseCode = "404", description = "일정 없음")
    })
    @PostMapping("/{scheduleId}/items")
    public ResponseEntity<ScheduleItemResponse> addItem(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId,
            @RequestBody @Valid ScheduleItemCreateRequest request) {
        return ResponseEntity.ok(scheduleService.addItem(scheduleId, request));
    }

    @Operation(summary = "일정 아이템 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "다른 일정의 아이템"),
            @ApiResponse(responseCode = "404", description = "아이템 없음")
    })
    @PatchMapping("/{scheduleId}/items/{itemId}")
    public ResponseEntity<ScheduleItemResponse> updateItem(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId,
            @Parameter(description = "아이템 ID", required = true) @PathVariable Long itemId,
            @RequestBody @Valid ScheduleItemUpdateRequest request) {
        return ResponseEntity.ok(scheduleService.updateItem(scheduleId, itemId, request));
    }

    @Operation(summary = "일정 아이템 위치 이동 (D&D)", description = "아이템 하나를 목표 위치(day, orderIndex)로 이동합니다. 서버가 나머지 아이템 순서를 자동 조정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "이동 성공"),
            @ApiResponse(responseCode = "404", description = "아이템 없음")
    })
    @PatchMapping("/{scheduleId}/items/{itemId}/position")
    public ResponseEntity<Void> moveItem(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId,
            @Parameter(description = "아이템 ID", required = true) @PathVariable Long itemId,
            @RequestBody @Valid ScheduleItemMoveRequest request) {
        scheduleService.moveItem(scheduleId, itemId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "일정 아이템 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "다른 일정의 아이템"),
            @ApiResponse(responseCode = "404", description = "아이템 없음")
    })
    @DeleteMapping("/{scheduleId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId,
            @Parameter(description = "아이템 ID", required = true) @PathVariable Long itemId) {
        scheduleService.deleteItem(scheduleId, itemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "딥링크 클릭 추적", description = "아이템의 딥링크 클릭 이벤트를 기록합니다.")
    @ApiResponse(responseCode = "204", description = "기록 성공")
    @PostMapping("/{scheduleId}/items/{itemId}/deeplink-click")
    public ResponseEntity<Void> trackDeepLinkClick(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId,
            @Parameter(description = "아이템 ID", required = true) @PathVariable Long itemId) {
        scheduleService.trackDeepLinkClick(scheduleId, itemId);
        return ResponseEntity.noContent().build();
    }
}

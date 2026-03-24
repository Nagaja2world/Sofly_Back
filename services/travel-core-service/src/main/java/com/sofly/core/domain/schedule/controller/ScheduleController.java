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

import java.util.List;

@Tag(name = "Schedule", description = "일정 관리 API")
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ── 조회 ────────────────────────────────────────────────

    @Operation(summary = "워크스페이스의 일정 목록 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<List<ScheduleSummaryResponse>> getSchedules(
            @Parameter(description = "워크스페이스 ID", required = true) @RequestParam Long workspaceId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByWorkspace(workspaceId));
    }

    @Operation(summary = "일정 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "일정 없음")
    })
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId) {
        return ResponseEntity.ok(scheduleService.getSchedule(scheduleId));
    }

    @Operation(summary = "워크스페이스의 최신 일정 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "일정 없음")
    })
    @GetMapping("/latest")
    public ResponseEntity<ScheduleResponse> getLatestSchedule(
            @Parameter(description = "워크스페이스 ID", required = true) @RequestParam Long workspaceId) {
        return ResponseEntity.ok(scheduleService.getLatestSchedule(workspaceId));
    }

    // ── 생성 ────────────────────────────────────────────────

    @Operation(summary = "일정 생성")
    @ApiResponse(responseCode = "200", description = "생성 성공")
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @RequestBody @Valid ScheduleCreateRequest request) {
        return ResponseEntity.ok(scheduleService.createSchedule(request));
    }

    @Operation(summary = "일정 포크 (복사)", description = "기존 일정을 복사하여 새 일정을 생성합니다. title 미입력 시 원본 제목을 사용합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포크 성공"),
            @ApiResponse(responseCode = "404", description = "원본 일정 없음")
    })
    @PostMapping("/{scheduleId}/fork")
    public ResponseEntity<ScheduleResponse> forkSchedule(
            @Parameter(description = "원본 일정 ID", required = true) @PathVariable Long scheduleId,
            @Parameter(description = "새 일정 제목 (선택)") @RequestParam(required = false) String title) {
        return ResponseEntity.ok(scheduleService.forkSchedule(scheduleId, title));
    }

    // ── 수정 ────────────────────────────────────────────────

    @Operation(summary = "일정 제목 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "일정 없음")
    })
    @PatchMapping("/{scheduleId}/title")
    public ResponseEntity<ScheduleResponse> updateTitle(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId,
            @Parameter(description = "변경할 제목", required = true) @RequestParam String title) {
        return ResponseEntity.ok(scheduleService.updateScheduleTitle(scheduleId, title));
    }

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
    @PatchMapping("/schedules/{scheduleId}/items/{itemId}")
    public ResponseEntity<ScheduleItemResponse> updateItem(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId,
            @Parameter(description = "아이템 ID", required = true) @PathVariable Long itemId,
            @RequestBody @Valid ScheduleItemUpdateRequest request) {
        return ResponseEntity.ok(scheduleService.updateItem(scheduleId, itemId, request));
    }

    @Operation(summary = "일정 아이템 순서 변경 (D&D)", description = "변경된 전체 순서 목록을 한 번에 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "순서 변경 성공"),
            @ApiResponse(responseCode = "400", description = "다른 일정의 아이템 포함"),
            @ApiResponse(responseCode = "404", description = "아이템 없음")
    })
    @PatchMapping("/{scheduleId}/items/reorder")
    public ResponseEntity<Void> reorderItems(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId,
            @RequestBody @Valid ScheduleItemReorderRequest request) {
        scheduleService.reorderItems(scheduleId, request);
        return ResponseEntity.noContent().build();
    }

    // ── 삭제 ────────────────────────────────────────────────

    @Operation(summary = "일정 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "일정 없음")
    })
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "일정 아이템 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "다른 일정의 아이템"),
            @ApiResponse(responseCode = "404", description = "아이템 없음")
    })
    @DeleteMapping("/schedules/{scheduleId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId,
            @Parameter(description = "아이템 ID", required = true) @PathVariable Long itemId) {
        scheduleService.deleteItem(scheduleId, itemId);
        return ResponseEntity.noContent().build();
    }

    // ── 딥링크 ──────────────────────────────────────────────

    @Operation(summary = "딥링크 클릭 추적", description = "아이템의 딥링크 클릭 이벤트를 기록합니다.")
    @ApiResponse(responseCode = "204", description = "기록 성공")
    @PostMapping("/schedules/{scheduleId}/items/{itemId}/deeplink-click")
    public ResponseEntity<Void> trackDeepLinkClick(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId,
            @Parameter(description = "아이템 ID", required = true) @PathVariable Long itemId) {
        scheduleService.trackDeepLinkClick(scheduleId, itemId);
        return ResponseEntity.noContent().build();
    }
}
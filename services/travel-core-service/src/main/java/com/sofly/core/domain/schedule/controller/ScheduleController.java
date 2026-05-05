package com.sofly.core.domain.schedule.controller;

import com.sofly.core.domain.schedule.dto.ScheduleCreateRequest;
import com.sofly.core.domain.schedule.dto.ScheduleMapResponse;
import com.sofly.core.domain.schedule.dto.ScheduleResponse;
import com.sofly.core.domain.schedule.dto.ScheduleSummaryResponse;
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

@Tag(name = "Schedule", description = "일정 생성·조회·삭제 API")
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

    // ── Map ────────────────────────────────────────────────

    @Operation(summary = "일정 지도 핀 조회", description = "좌표(위경도)가 등록된 아이템을 일차별로 그룹핑하여 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "일정 없음")
    })
    @GetMapping("/{scheduleId}/map")
    public ResponseEntity<ScheduleMapResponse> getMapPins(
            @Parameter(description = "일정 ID", required = true) @PathVariable Long scheduleId) {
        return ResponseEntity.ok(scheduleService.getScheduleMap(scheduleId));
    }
}
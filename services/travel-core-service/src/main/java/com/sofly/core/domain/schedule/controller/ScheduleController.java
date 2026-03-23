package com.sofly.core.domain.schedule.controller;

import com.sofly.core.domain.schedule.dto.*;
import com.sofly.core.domain.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ── 조회 ────────────────────────────────────────────────

    // GET /api/v1/schedules?workspaceId=1
    @GetMapping
    public ResponseEntity<List<ScheduleSummaryResponse>> getSchedules(
            @RequestParam Long workspaceId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByWorkspace(workspaceId));
    }

    // GET /api/v1/schedules/{scheduleId}
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @PathVariable Long scheduleId) {
        return ResponseEntity.ok(scheduleService.getSchedule(scheduleId));
    }

    // GET /api/v1/schedules/latest?workspaceId=1
    @GetMapping("/latest")
    public ResponseEntity<ScheduleResponse> getLatestSchedule(
            @RequestParam Long workspaceId) {
        return ResponseEntity.ok(scheduleService.getLatestSchedule(workspaceId));
    }

    // ── 생성 ────────────────────────────────────────────────

    // POST /api/v1/schedules
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @RequestBody @Valid ScheduleCreateRequest request) {
        return ResponseEntity.ok(scheduleService.createSchedule(request));
    }

    // POST /api/v1/schedules/{scheduleId}/fork
    @PostMapping("/{scheduleId}/fork")
    public ResponseEntity<ScheduleResponse> forkSchedule(
            @PathVariable Long scheduleId,
            @RequestParam(required = false) String title) {
        return ResponseEntity.ok(scheduleService.forkSchedule(scheduleId, title));
    }

    // ── 수정 ────────────────────────────────────────────────

    // PATCH /api/v1/schedules/{scheduleId}/title
    @PatchMapping("/{scheduleId}/title")
    public ResponseEntity<ScheduleResponse> updateTitle(
            @PathVariable Long scheduleId,
            @RequestParam String title) {
        return ResponseEntity.ok(scheduleService.updateScheduleTitle(scheduleId, title));
    }

    // POST /api/v1/schedules/{scheduleId}/items
    @PostMapping("/{scheduleId}/items")
    public ResponseEntity<ScheduleItemResponse> addItem(
            @PathVariable Long scheduleId,
            @RequestBody @Valid ScheduleItemCreateRequest request) {
        return ResponseEntity.ok(scheduleService.addItem(scheduleId, request));
    }

    // PATCH /api/v1/schedules/items/{itemId}
    @PatchMapping("/schedules/{scheduleId}/items/{itemId}")
    public ResponseEntity<ScheduleItemResponse> updateItem(
            @PathVariable Long scheduleId,
            @PathVariable Long itemId,
            @RequestBody @Valid ScheduleItemUpdateRequest request) {
        return ResponseEntity.ok(scheduleService.updateItem(scheduleId, itemId, request));
    }

    // PATCH /api/v1/schedules/{scheduleId}/items/reorder
    @PatchMapping("/{scheduleId}/items/reorder")
    public ResponseEntity<Void> reorderItems(
            @PathVariable Long scheduleId,
            @RequestBody @Valid ScheduleItemReorderRequest request) {
        scheduleService.reorderItems(scheduleId, request);
        return ResponseEntity.noContent().build();
    }

    // ── 삭제 ────────────────────────────────────────────────

    // DELETE /api/v1/schedules/{scheduleId}
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }

    // DELETE /schedules/{scheduleId}/items/{itemId}
    @DeleteMapping("/schedules/{scheduleId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long scheduleId,
            @PathVariable Long itemId) {
        scheduleService.deleteItem(scheduleId ,itemId);
        return ResponseEntity.noContent().build();
    }

    // ── 딥링크 ──────────────────────────────────────────────

    // POST /schedules/{scheduleId}/items/{itemId}/deeplink-click
    @PostMapping("/schedules/{scheduleId}/items/{itemId}/deeplink-click")
    public ResponseEntity<Void> trackDeepLinkClick(
            @PathVariable Long scheduleId,
            @PathVariable Long itemId) {
        scheduleService.trackDeepLinkClick(scheduleId, itemId);
        return ResponseEntity.noContent().build();
    }
}
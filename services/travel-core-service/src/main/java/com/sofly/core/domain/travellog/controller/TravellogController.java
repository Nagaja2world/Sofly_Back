package com.sofly.core.domain.travellog.controller;

import com.sofly.core.domain.travellog.dto.TravellogCreateRequest;
import com.sofly.core.domain.travellog.dto.TravellogResponse;
import com.sofly.core.domain.travellog.dto.TravellogUpdateRequest;
import com.sofly.core.domain.travellog.service.TravellogService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "TravelLog", description = "여행기 CRUD API")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/travellogs")
@RequiredArgsConstructor
public class TravellogController {

    private final TravellogService travellogService;

    @Operation(summary = "여행기 목록 조회", description = "워크스페이스의 여행기 목록을 여행 날짜 오름차순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TravellogResponse>>> getTravelLogs(@PathVariable Long workspaceId) {
        return ResponseEntity.ok(ApiResponse.success(travellogService.getTravelLogs(workspaceId)));
    }

    @Operation(summary = "여행기 단건 조회", description = "여행기 상세 정보와 첨부 사진을 조회합니다.")
    @GetMapping("/{logId}")
    public ResponseEntity<ApiResponse<TravellogResponse>> getTravelLog(
            @PathVariable Long workspaceId,
            @PathVariable Long logId) {
        return ResponseEntity.ok(ApiResponse.success(travellogService.getTravelLog(workspaceId, logId)));
    }

    @Operation(summary = "여행기 생성", description = "새 여행기를 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<TravellogResponse>> createTravelLog(
            @PathVariable Long workspaceId,
            @RequestBody @Valid TravellogCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(travellogService.createTravelLog(workspaceId, userId, request)));
    }

    @Operation(summary = "여행기 수정", description = "여행기 내용을 부분 수정합니다. null 필드는 변경되지 않습니다.")
    @PatchMapping("/{logId}")
    public ResponseEntity<ApiResponse<TravellogResponse>> updateTravelLog(
            @PathVariable Long workspaceId,
            @PathVariable Long logId,
            @RequestBody TravellogUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(travellogService.updateTravelLog(workspaceId, logId, request)));
    }

    @Operation(summary = "여행기 삭제", description = "여행기를 삭제합니다.")
    @DeleteMapping("/{logId}")
    public ResponseEntity<Void> deleteTravelLog(
            @PathVariable Long workspaceId,
            @PathVariable Long logId) {
        travellogService.deleteTravelLog(workspaceId, logId);
        return ResponseEntity.noContent().build();
    }
}

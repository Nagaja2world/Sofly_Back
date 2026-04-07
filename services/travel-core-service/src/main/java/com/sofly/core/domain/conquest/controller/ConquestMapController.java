package com.sofly.core.domain.conquest.controller;

import com.sofly.core.domain.conquest.dto.request.BulkImportRequest;
import com.sofly.core.domain.conquest.dto.request.CityCreateRequest;
import com.sofly.core.domain.conquest.dto.request.StatusUpdateRequest;
import com.sofly.core.domain.conquest.dto.response.*;
import com.sofly.core.domain.conquest.service.ConquestMapService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Conquest Map", description = "정복 지도 - 방문 국가·도시 관리 및 여행 통계")
@RestController
@RequestMapping("/api/conquest")
@RequiredArgsConstructor
public class ConquestMapController {

    private final ConquestMapService conquestMapService;

    // ── 지도 전체 데이터 조회 ──────────────────────────────────────────

    @Operation(summary = "정복 지도 전체 조회", description = "내 방문 국가·도시 목록을 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ConquestMapResponse>> getConquestMap() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(conquestMapService.getConquestMap(userId)));
    }

    // ── 통계 패널 ──────────────────────────────────────────────────────

    @Operation(summary = "여행 통계 조회", description = "방문 국가 수, 도시 수, 총 여행 일수, 이동 거리, 대륙별 분포를 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ConquestStatsResponse>> getStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(conquestMapService.getStats(userId)));
    }

    // ── 국가 상태 수동 변경 ────────────────────────────────────────────

    @Operation(summary = "국가 방문 상태 변경", description = "국가 코드(ISO alpha-2)로 방문 상태를 직접 변경합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @PutMapping("/countries/{countryCode}/status")
    public ResponseEntity<ApiResponse<VisitedCountryResponse>> updateCountryStatus(
            @PathVariable String countryCode,
            @Valid @RequestBody StatusUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                conquestMapService.updateCountryStatus(userId, countryCode, request)));
    }

    // ── 도시 추가 / 상태 설정 ──────────────────────────────────────────

    @Operation(summary = "도시 추가 또는 상태 설정", description = "도시를 수동으로 추가하거나 기존 도시의 상태를 변경합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성/변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @PostMapping("/cities")
    public ResponseEntity<ApiResponse<VisitedCityResponse>> addOrUpdateCity(
            @Valid @RequestBody CityCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(conquestMapService.addOrUpdateCity(userId, request)));
    }

    // ── 도시 상태 변경 ─────────────────────────────────────────────────

    @Operation(summary = "도시 방문 상태 변경", description = "도시 ID로 방문 상태를 변경합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "도시를 찾을 수 없음")
    })
    @PutMapping("/cities/{cityId}/status")
    public ResponseEntity<ApiResponse<VisitedCityResponse>> updateCityStatus(
            @PathVariable Long cityId,
            @Valid @RequestBody StatusUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                conquestMapService.updateCityStatus(userId, cityId, request)));
    }

    // ── 과거 방문 일괄 등록 ────────────────────────────────────────────

    @Operation(summary = "방문 이력 일괄 등록", description = "과거 방문한 국가·도시를 한 번에 등록합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "등록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @PostMapping("/bulk-import")
    public ResponseEntity<Void> bulkImport(@Valid @RequestBody BulkImportRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        conquestMapService.bulkImport(userId, request);
        return ResponseEntity.noContent().build();
    }

    // ── 국가별 워크스페이스 목록 ───────────────────────────────────────

    @Operation(summary = "국가별 워크스페이스 조회", description = "특정 국가를 목적지로 하는 내 워크스페이스 목록을 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @GetMapping("/countries/{countryCode}/workspaces")
    public ResponseEntity<ApiResponse<List<WorkspaceConquestResponse>>> getWorkspacesByCountry(
            @PathVariable String countryCode) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                conquestMapService.getWorkspacesByCountry(userId, countryCode)));
    }

    // ── 여행 경로 전체 조회 ────────────────────────────────────────────

    @Operation(summary = "전체 여행 경로 조회", description = "내 모든 워크스페이스의 항공 경로를 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<List<TripRouteResponse>>> getTripRoutes() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(conquestMapService.getTripRoutes(userId)));
    }

    // ── 워크스페이스별 여행 경로 조회 ──────────────────────────────────

    @Operation(summary = "워크스페이스 여행 경로 조회", description = "특정 워크스페이스의 항공 경로를 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @GetMapping("/routes/workspaces/{workspaceId}")
    public ResponseEntity<ApiResponse<List<TripRouteResponse>>> getTripRoutesByWorkspace(
            @PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                conquestMapService.getTripRoutesByWorkspace(userId, workspaceId)));
    }
}

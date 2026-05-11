package com.sofly.core.domain.workspace.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sofly.core.domain.workspace.dto.request.SaveFlightRequest;
import com.sofly.core.domain.workspace.dto.response.SavedFlightResponse;
import com.sofly.core.domain.workspace.service.WorkspaceService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Workspace Flight", description = "워크스페이스 항공편 저장·조회")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/flights")
@RequiredArgsConstructor
public class WorkspaceFlightController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "항공편 저장", description = "워크스페이스에 항공편을 저장합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "저장 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값이 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SavedFlightResponse>> saveFlight(
            @PathVariable Long workspaceId,
            @Valid @RequestBody SaveFlightRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(workspaceService.saveFlight(userId, workspaceId, request)));
    }

    @Operation(summary = "저장된 항공편 목록 조회", description = "워크스페이스에 저장된 항공편 목록을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedFlightResponse>>> getFlights(
            @PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(workspaceService.getFlights(userId, workspaceId)));
    }

    @Operation(summary = "저장된 항공편 삭제", description = "워크스페이스에 저장된 항공편을 삭제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스 또는 항공편을 찾을 수 없음")
    })
    @DeleteMapping("/{flightId}")
    public ResponseEntity<Void> deleteFlight(
            @PathVariable Long workspaceId,
            @PathVariable Long flightId) {
        Long userId = SecurityUtils.getCurrentUserId();
        workspaceService.deleteFlight(userId, workspaceId, flightId);
        return ResponseEntity.noContent().build();
    }
}

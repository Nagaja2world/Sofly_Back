package com.sofly.core.domain.workspace.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofly.core.domain.workspace.dto.request.SavePlaceRequest;
import com.sofly.core.domain.workspace.dto.response.SavedPlaceResponse;
import com.sofly.core.domain.workspace.service.WorkspaceService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Workspace Place", description = "워크스페이스 장소 저장·조회·삭제")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/places")
@RequiredArgsConstructor
public class WorkspacePlaceController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "장소 저장", description = "워크스페이스에 Google Places 기반 장소를 저장합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "저장 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값이 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 저장된 장소")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SavedPlaceResponse>> savePlace(
            @PathVariable Long workspaceId,
            @Valid @RequestBody SavePlaceRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(workspaceService.savePlace(userId, workspaceId, request)));
    }

    @Operation(summary = "저장된 장소 목록 조회", description = "워크스페이스에 저장된 장소 목록을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedPlaceResponse>>> getSavedPlaces(
            @PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(workspaceService.getSavedPlaces(userId, workspaceId)));
    }

    @Operation(summary = "저장된 장소 삭제", description = "워크스페이스에 저장된 장소를 삭제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스 또는 저장된 장소를 찾을 수 없음")
    })
    @DeleteMapping("/{savedPlaceId}")
    public ResponseEntity<Void> deleteSavedPlace(
            @PathVariable Long workspaceId,
            @PathVariable Long savedPlaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        workspaceService.deleteSavedPlace(userId, workspaceId, savedPlaceId);
        return ResponseEntity.noContent().build();
    }
}

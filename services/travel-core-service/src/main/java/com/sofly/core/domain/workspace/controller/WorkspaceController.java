package com.sofly.core.domain.workspace.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.sofly.core.domain.workspace.dto.request.CreateWorkspaceRequest;
import com.sofly.core.domain.workspace.dto.request.UpdateWorkspaceRequest;
import com.sofly.core.domain.workspace.dto.response.WorkspaceResponse;
import com.sofly.core.domain.workspace.service.WorkspaceService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Workspace", description = "워크스페이스 생성·관리")
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "워크스페이스 생성", description = "새로운 워크스페이스(여행)를 생성합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값이 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(workspaceService.createWorkspace(userId, request)));
    }

    @Operation(summary = "내 워크스페이스 목록 조회", description = "내가 소유하거나 참여 중인 워크스페이스 목록을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> getMyWorkspaces() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(workspaceService.getMyWorkspaces(userId)));
    }

    @Operation(summary = "워크스페이스 상세 조회", description = "워크스페이스 ID로 상세 정보를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @GetMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspace(
            @PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(workspaceService.getWorkspace(userId, workspaceId)));
    }

    @Operation(summary = "워크스페이스 수정", description = "워크스페이스 정보를 수정합니다. 소유자만 가능합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값이 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유자가 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @PutMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateWorkspace(
            @PathVariable Long workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(workspaceService.updateWorkspace(userId, workspaceId, request)));
    }

    @Operation(summary = "워크스페이스 커버 이미지 업로드", description = "워크스페이스 커버 이미지를 S3에 업로드합니다. 소유자만 가능합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유자가 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @PatchMapping(value = "/{workspaceId}/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<WorkspaceResponse>> uploadCoverImage(
            @PathVariable Long workspaceId,
            @RequestPart("image") MultipartFile image) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(workspaceService.uploadCoverImage(userId, workspaceId, image)));
    }

    @Operation(summary = "워크스페이스 삭제", description = "워크스페이스를 삭제합니다. 소유자만 가능합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유자가 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> deleteWorkspace(
            @PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        workspaceService.deleteWorkspace(userId, workspaceId);
        return ResponseEntity.noContent().build();
    }
}

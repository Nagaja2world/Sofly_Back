package com.sofly.core.domain.workspace.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sofly.core.domain.workspace.dto.response.InviteCodeResponse;
import com.sofly.core.domain.workspace.dto.response.WorkspaceResponse;
import com.sofly.core.domain.workspace.service.WorkspaceService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Workspace Invite", description = "워크스페이스 초대·공유")
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceInviteController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "초대 코드 생성", description = "워크스페이스 초대 링크를 생성합니다. 소유자만 가능합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "소유자가 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @PostMapping("/{workspaceId}/invite-code")
    public ResponseEntity<ApiResponse<InviteCodeResponse>> generateInviteCode(
            @PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(workspaceService.generateInviteCode(userId, workspaceId)));
    }

    @Operation(summary = "초대 코드로 참여", description = "초대 코드를 통해 워크스페이스 멤버로 참여합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참여 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 초대 코드"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 워크스페이스 멤버")
    })
    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> joinWorkspace(
            @PathVariable String inviteCode) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(workspaceService.joinWorkspace(userId, inviteCode)));
    }
}

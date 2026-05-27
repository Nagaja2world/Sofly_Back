package com.sofly.core.domain.workspace.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofly.core.domain.workspace.dto.response.InvitationResponse;
import com.sofly.core.domain.workspace.dto.response.WorkspaceMemberResponse;
import com.sofly.core.domain.workspace.service.WorkspaceService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Invitation", description = "초대 수락·거절")
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "내 초대 목록 조회", description = "나에게 온 PENDING 상태의 초대 목록을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> getMyInvitations() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(workspaceService.getMyInvitations(userId)));
    }

    @Operation(summary = "초대 수락", description = "초대를 수락하고 워크스페이스 멤버(VIEWER)로 참여합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수락 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "만료된 초대"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "초대를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 초대 또는 이미 멤버")
    })
    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<ApiResponse<WorkspaceMemberResponse>> acceptInvitation(
            @PathVariable Long invitationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(workspaceService.acceptInvitation(userId, invitationId)));
    }

    @Operation(summary = "초대 거절", description = "초대를 거절합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거절 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "초대를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 초대")
    })
    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectInvitation(
            @PathVariable Long invitationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        workspaceService.rejectInvitation(userId, invitationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

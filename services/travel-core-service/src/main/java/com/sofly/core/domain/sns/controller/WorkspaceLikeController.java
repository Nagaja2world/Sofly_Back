package com.sofly.core.domain.sns.controller;

import com.sofly.core.domain.sns.service.LikeService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "WorkspaceLike", description = "워크스페이스 좋아요")
@RestController
@RequestMapping("/api/sns/workspaces")
@RequiredArgsConstructor
public class WorkspaceLikeController {

    private final LikeService likeService;

    @Operation(summary = "좋아요")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스 또는 유저 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 좋아요")
    })
    @PostMapping("/{workspaceId}/likes")
    public ResponseEntity<ApiResponse<Void>> like(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        likeService.like(userId, workspaceId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "좋아요 취소")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "좋아요 취소 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "좋아요 없음")
    })
    @DeleteMapping("/{workspaceId}/likes")
    public ResponseEntity<Void> unlike(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        likeService.unlike(userId, workspaceId);
        return ResponseEntity.noContent().build();
    }
}

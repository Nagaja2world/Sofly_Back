package com.sofly.core.domain.sns.controller;

import com.sofly.core.domain.sns.dto.CommentCreateRequest;
import com.sofly.core.domain.sns.dto.CommentResponse;
import com.sofly.core.domain.sns.dto.CommentUpdateRequest;
import com.sofly.core.domain.sns.service.CommentService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "WorkspaceComment", description = "워크스페이스 댓글")
@RestController
@RequestMapping("/api/sns/workspaces/{workspaceId}/comments")
@RequiredArgsConstructor
public class WorkspaceCommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 목록 조회", description = "미인증 접근 가능. 기본 20개, 생성일 오름차순. PRIVATE 워크스페이스는 접근 불가, FOLLOWERS_ONLY는 팔로워 이상만 조회 가능.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getComments(
            @PathVariable Long workspaceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Long viewerId = SecurityUtils.tryGetCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(commentService.getComments(workspaceId, viewerId, pageable)));
    }

    @Operation(summary = "댓글 작성")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId,
            @Valid @RequestBody CommentCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(commentService.createComment(userId, workspaceId, request.content())));
    }

    @Operation(summary = "댓글 수정 (본인만)")
    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long workspaceId,
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                commentService.updateComment(userId, commentId, request.content())));
    }

    @Operation(summary = "댓글 삭제 (본인만)")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long workspaceId,
            @Parameter(description = "댓글 ID") @PathVariable Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        commentService.deleteComment(userId, commentId);
        return ResponseEntity.noContent().build();
    }
}

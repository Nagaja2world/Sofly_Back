package com.sofly.core.domain.sns.controller;

import com.sofly.core.domain.sns.dto.SnsPostResponse;
import com.sofly.core.domain.sns.dto.SnsPostUpdateRequest;
import com.sofly.core.domain.sns.entity.SnsPost;
import com.sofly.core.domain.sns.service.SnsPostService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "SNS Post", description = "SNS 게시물 (워크스페이스당 1개)")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/sns/post")
@RequiredArgsConstructor
public class SnsPostController {

    private final SnsPostService snsPostService;

    @Operation(summary = "SNS 카드 생성",
               description = "워크스페이스당 1개. 사진(최대 10장) + 텍스트 + 공개범위(PUBLIC/FOLLOWERS_ONLY/PRIVATE)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SnsPostResponse>> createPost(
            @PathVariable Long workspaceId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(required = false) String content,
            @RequestParam SnsPost.Visibility visibility) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        snsPostService.createPost(workspaceId, userId, files, content, visibility)));
    }

    @Operation(summary = "SNS 카드 조회", description = "워크스페이스의 SNS 카드 전체 정보(이미지 전체) 반환")
    @GetMapping
    public ResponseEntity<ApiResponse<SnsPostResponse>> getPost(
            @PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(snsPostService.getPost(workspaceId, userId)));
    }

    @Operation(summary = "SNS 카드 수정",
               description = "텍스트·공개범위 수정. files 전달 시 기존 이미지 전체 교체.")
    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SnsPostResponse>> updatePost(
            @PathVariable Long workspaceId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) SnsPost.Visibility visibility) {
        Long userId = SecurityUtils.getCurrentUserId();
        SnsPostUpdateRequest request = new SnsPostUpdateRequest(content, visibility);
        return ResponseEntity.ok(ApiResponse.success(
                snsPostService.updatePost(workspaceId, userId, files, request)));
    }

    @Operation(summary = "SNS 카드 삭제", description = "작성자 본인만 삭제 가능. S3 이미지도 함께 삭제됩니다.")
    @DeleteMapping
    public ResponseEntity<Void> deletePost(@PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        snsPostService.deletePost(workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}

package com.sofly.core.domain.sns.controller;

import com.sofly.core.domain.sns.dto.SnsPostResponse;
import com.sofly.core.domain.sns.dto.SnsPostUpdateRequest;
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
               description = "워크스페이스당 1개. 직접 업로드 파일(files) + 공유앨범 참조(albumPhotoIds) 합산 최대 10장. 순서: albumPhotoIds → files.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SnsPostResponse>> createPost(
            @PathVariable Long workspaceId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(required = false) List<Long> albumPhotoIds,
            @RequestParam(required = false) String content) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        snsPostService.createPost(workspaceId, userId, files, albumPhotoIds, content)));
    }

    @Operation(summary = "SNS 카드 조회", description = "워크스페이스의 SNS 카드 전체 정보(이미지 전체) 반환")
    @GetMapping
    public ResponseEntity<ApiResponse<SnsPostResponse>> getPost(
            @PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(snsPostService.getPost(workspaceId, userId)));
    }

    @Operation(summary = "SNS 카드 수정",
               description = """
                       텍스트 수정 + 이미지 부분/전체 업데이트.
                       최종 순서: keepImageIds → albumPhotoIds → files.
                       keepImageIds 전달 시: 해당 ID 유지(순서 재정렬) + 나머지 삭제 + albumPhotoIds + files 추가.
                       keepImageIds 미전달 + (albumPhotoIds || files) 전달 시: 기존 이미지 전체 교체.
                       albumPhotoIds: 공유앨범 사진 ID 목록 (S3 복사 방식으로 추가).
                       """)
    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SnsPostResponse>> updatePost(
            @PathVariable Long workspaceId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(required = false) List<Long> keepImageIds,
            @RequestParam(required = false) List<Long> albumPhotoIds,
            @RequestParam(required = false) String content) {
        Long userId = SecurityUtils.getCurrentUserId();
        SnsPostUpdateRequest request = new SnsPostUpdateRequest(content);
        return ResponseEntity.ok(ApiResponse.success(
                snsPostService.updatePost(workspaceId, userId, files, keepImageIds, albumPhotoIds, request)));
    }

    @Operation(summary = "SNS 카드 삭제", description = "작성자 또는 워크스페이스 OWNER만 삭제 가능. S3 이미지도 함께 삭제됩니다.")
    @DeleteMapping
    public ResponseEntity<Void> deletePost(@PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        snsPostService.deletePost(workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}

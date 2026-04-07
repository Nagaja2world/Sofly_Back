package com.sofly.core.domain.album.controller;

import com.sofly.core.domain.album.dto.*;
import com.sofly.core.domain.album.service.AlbumService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Album", description = "워크스페이스 공유 앨범 API")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @Operation(summary = "앨범 조회", description = "워크스페이스 앨범과 사진 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AlbumResponse>> getAlbum(@PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(albumService.getAlbum(workspaceId, userId)));
    }

    @Operation(summary = "업로드 URL 발급", description = "S3 직접 업로드를 위한 Presigned PUT URL을 발급합니다.")
    @PostMapping("/photos/presigned")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generateUploadUrl(
            @PathVariable Long workspaceId,
            @Valid @RequestBody PresignedUrlRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(albumService.generateUploadUrl(workspaceId, userId, request)));
    }

    @Operation(summary = "사진 저장", description = "S3 업로드 완료 후 사진 정보를 DB에 저장합니다.")
    @PostMapping("/photos")
    public ResponseEntity<ApiResponse<PhotoResponse>> savePhoto(
            @PathVariable Long workspaceId,
            @Valid @RequestBody PhotoSaveRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(albumService.savePhoto(workspaceId, userId, request)));
    }

    @Operation(summary = "사진 삭제", description = "사진을 S3와 DB에서 삭제합니다.")
    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @PathVariable Long workspaceId,
            @PathVariable Long photoId) {
        Long userId = SecurityUtils.getCurrentUserId();
        albumService.deletePhoto(workspaceId, userId, photoId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "다운로드 URL 발급", description = "사진 다운로드를 위한 Presigned GET URL을 발급합니다. (5분 유효)")
    @GetMapping("/photos/{photoId}/download")
    public ResponseEntity<ApiResponse<DownloadUrlResponse>> generateDownloadUrl(
            @PathVariable Long workspaceId,
            @PathVariable Long photoId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(albumService.generateDownloadUrl(workspaceId, userId, photoId)));
    }
}

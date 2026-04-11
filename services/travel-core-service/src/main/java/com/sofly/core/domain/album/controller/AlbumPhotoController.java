package com.sofly.core.domain.album.controller;

import com.sofly.core.domain.album.dto.DownloadUrlResponse;
import com.sofly.core.domain.album.dto.PhotoResponse;
import com.sofly.core.domain.album.service.AlbumService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Album Photo", description = "앨범 사진 업로드·삭제·다운로드 API")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/album/photos")
@RequiredArgsConstructor
public class AlbumPhotoController {

    private final AlbumService albumService;

    @Operation(summary = "사진 업로드", description = "사진을 한 장 또는 여러 장 업로드합니다. S3 저장 후 DB에 자동 저장됩니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<PhotoResponse>>> uploadPhotos(
            @PathVariable Long workspaceId,
            @RequestPart("files") List<MultipartFile> files) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(albumService.uploadPhotos(workspaceId, userId, files)));
    }

    @Operation(summary = "사진 삭제", description = "사진을 S3와 DB에서 삭제합니다.")
    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long workspaceId,
            @PathVariable Long photoId) {
        Long userId = SecurityUtils.getCurrentUserId();
        albumService.deletePhoto(workspaceId, userId, photoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "다운로드 URL 발급", description = "사진 다운로드를 위한 Presigned GET URL을 발급합니다. (5분 유효)")
    @GetMapping("/{photoId}/download")
    public ResponseEntity<ApiResponse<DownloadUrlResponse>> generateDownloadUrl(
            @PathVariable Long workspaceId,
            @PathVariable Long photoId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(albumService.generateDownloadUrl(workspaceId, userId, photoId)));
    }
}

package com.sofly.core.domain.travellog.controller;

import com.sofly.core.domain.travellog.dto.TravellogPhotoLinkRequest;
import com.sofly.core.domain.travellog.dto.TravellogResponse;
import com.sofly.core.domain.travellog.service.TravellogService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "TravelLog Photo", description = "여행 기록 사진 첨부·해제 API")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/travellogs/{logId}/photos")
@RequiredArgsConstructor
public class TravellogPhotoController {

    private final TravellogService travellogService;

    @Operation(summary = "사진 업로드 후 첨부", description = "새 사진을 S3에 업로드하고 여행기에 첨부합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TravellogResponse>> uploadAndAttachPhotos(
            @PathVariable Long workspaceId,
            @PathVariable Long logId,
            @RequestPart("files") List<MultipartFile> files) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                travellogService.uploadAndAttachPhotos(workspaceId, userId, logId, files)));
    }

    @Operation(summary = "앨범 사진 첨부", description = "워크스페이스 앨범에 이미 업로드된 사진을 여행기에 첨부합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<TravellogResponse>> attachAlbumPhotos(
            @PathVariable Long workspaceId,
            @PathVariable Long logId,
            @RequestBody @Valid TravellogPhotoLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                travellogService.attachAlbumPhotos(workspaceId, logId, request.photoIds())));
    }

    @Operation(summary = "첨부 사진 해제", description = "여행기에서 사진 첨부를 해제합니다. 앨범에서 사진이 삭제되지는 않습니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<TravellogResponse>> removePhotos(
            @PathVariable Long workspaceId,
            @PathVariable Long logId,
            @RequestBody @Valid TravellogPhotoLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                travellogService.removePhotos(workspaceId, logId, request.photoIds())));
    }
}

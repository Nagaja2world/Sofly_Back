package com.sofly.core.domain.album.controller;

import com.sofly.core.domain.album.dto.AlbumResponse;
import com.sofly.core.domain.album.service.AlbumService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Album", description = "워크스페이스 앨범 조회 API")
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
}

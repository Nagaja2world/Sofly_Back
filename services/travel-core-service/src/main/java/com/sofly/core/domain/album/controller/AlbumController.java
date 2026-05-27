package com.sofly.core.domain.album.controller;

import com.sofly.core.domain.album.dto.AlbumResponse;
import com.sofly.core.domain.album.service.AlbumService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Album", description = "워크스페이스 앨범 조회 API")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @Operation(summary = "앨범 조회", description = "워크스페이스 앨범과 사진 목록을 페이징으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AlbumResponse>> getAlbum(
            @PathVariable Long workspaceId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                albumService.getAlbum(workspaceId, userId, PageRequest.of(page, size))
        ));
    }
}

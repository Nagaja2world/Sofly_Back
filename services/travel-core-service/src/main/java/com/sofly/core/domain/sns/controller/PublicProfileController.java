package com.sofly.core.domain.sns.controller;

import com.sofly.core.domain.sns.dto.PublicUserProfileResponse;
import com.sofly.core.domain.sns.service.PublicProfileService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "PublicProfile", description = "공개 프로필 조회")
@RestController
@RequestMapping("/api/sns/users")
@RequiredArgsConstructor
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    @Operation(summary = "공개 프로필 조회",
               description = "미인증 접근 가능. 공개 워크스페이스 목록 포함. 초대 코드 미노출.")
    @GetMapping("/{targetUserId}/profile")
    public ResponseEntity<ApiResponse<PublicUserProfileResponse>> getProfile(
            @Parameter(description = "조회할 유저 ID") @PathVariable Long targetUserId,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long viewerId = SecurityUtils.tryGetCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                publicProfileService.getProfile(targetUserId, viewerId, pageable)));
    }
}

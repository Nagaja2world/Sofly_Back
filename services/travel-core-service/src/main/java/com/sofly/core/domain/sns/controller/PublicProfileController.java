package com.sofly.core.domain.sns.controller;

import com.sofly.core.domain.sns.dto.PublicUserProfileResponse;
import com.sofly.core.domain.sns.service.PublicProfileService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "PublicProfile", description = "공개 프로필 조회")
@RestController
@RequestMapping("/api/sns/users")
@RequiredArgsConstructor
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    @Operation(summary = "공개 프로필 조회",
               description = "미인증 접근 가능. page는 0부터 시작합니다. 공개 워크스페이스 목록 포함. 초대 코드 미노출.")
    @GetMapping("/{targetUserId}/profile")
    public ResponseEntity<ApiResponse<PublicUserProfileResponse>> getProfile(
            @Parameter(description = "조회할 유저 ID") @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long viewerId = SecurityUtils.tryGetCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                publicProfileService.getProfile(targetUserId, viewerId, pageable)));
    }
}

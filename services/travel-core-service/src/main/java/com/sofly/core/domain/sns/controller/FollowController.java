package com.sofly.core.domain.sns.controller;

import com.sofly.core.domain.sns.dto.FollowStatsResponse;
import com.sofly.core.domain.sns.service.FollowService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Follow", description = "팔로우/언팔로우 관리")
@RestController
@RequestMapping("/api/sns/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "팔로우")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "팔로우 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신 팔로우 불가"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유저 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 팔로우 중")
    })
    @PostMapping("/{targetUserId}/follow")
    public ResponseEntity<ApiResponse<Void>> follow(
            @Parameter(description = "팔로우할 유저 ID") @PathVariable Long targetUserId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        followService.follow(currentUserId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "언팔로우")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "언팔로우 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팔로우 관계 없음")
    })
    @DeleteMapping("/{targetUserId}/follow")
    public ResponseEntity<Void> unfollow(
            @Parameter(description = "언팔로우할 유저 ID") @PathVariable Long targetUserId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        followService.unfollow(currentUserId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "팔로우 통계 조회",
               description = "팔로워 수, 팔로잉 수, 현재 사용자의 팔로우 여부 반환. 미인증 시 isFollowing=false.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/{targetUserId}/follow-stats")
    public ResponseEntity<ApiResponse<FollowStatsResponse>> getFollowStats(
            @Parameter(description = "조회할 유저 ID") @PathVariable Long targetUserId) {
        Long viewerId = SecurityUtils.tryGetCurrentUserId();
        FollowStatsResponse stats = new FollowStatsResponse(
                followService.getFollowerCount(targetUserId),
                followService.getFollowingCount(targetUserId),
                viewerId != null && followService.isFollowing(viewerId, targetUserId)
        );
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

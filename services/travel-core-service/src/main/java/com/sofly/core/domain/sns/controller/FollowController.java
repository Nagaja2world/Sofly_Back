package com.sofly.core.domain.sns.controller;

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

import java.util.List;

@Tag(name = "Follow", description = "팔로우/언팔로우 관리")
@RestController
@RequestMapping("/api/v1/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "팔로우", description = "특정 유저를 팔로우합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "팔로우 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신을 팔로우할 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 팔로우 중")
    })
    @PostMapping("/{targetId}")
    public ResponseEntity<ApiResponse<Void>> follow(
            @Parameter(description = "팔로우할 유저 ID") @PathVariable Long targetId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        followService.follow(currentUserId, targetId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "언팔로우", description = "특정 유저를 언팔로우합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "언팔로우 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "팔로우 관계를 찾을 수 없음")
    })
    @DeleteMapping("/{targetId}")
    public ResponseEntity<Void> unfollow(
            @Parameter(description = "언팔로우할 유저 ID") @PathVariable Long targetId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        followService.unfollow(currentUserId, targetId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "팔로워 목록 조회", description = "특정 유저의 팔로워 닉네임 목록을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<List<String>>> getFollowers(
            @Parameter(description = "조회할 유저 ID") @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(followService.getFollowerList(userId)));
    }

    @Operation(summary = "팔로잉 목록 조회", description = "특정 유저의 팔로잉 닉네임 목록을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @GetMapping("/{userId}/followings")
    public ResponseEntity<ApiResponse<List<String>>> getFollowings(
            @Parameter(description = "조회할 유저 ID") @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(followService.getFollowingList(userId)));
    }

    @Operation(summary = "팔로워 수 조회", description = "특정 유저의 팔로워 수를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @GetMapping("/{userId}/followers/count")
    public ResponseEntity<ApiResponse<Long>> getFollowerCount(
            @Parameter(description = "조회할 유저 ID") @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(followService.getFollowerCount(userId)));
    }

    @Operation(summary = "팔로잉 수 조회", description = "특정 유저의 팔로잉 수를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @GetMapping("/{userId}/followings/count")
    public ResponseEntity<ApiResponse<Long>> getFollowingCount(
            @Parameter(description = "조회할 유저 ID") @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(followService.getFollowingCount(userId)));
    }

    @Operation(summary = "팔로우 여부 확인", description = "현재 로그인한 유저가 특정 유저를 팔로우하고 있는지 확인합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @GetMapping("/{targetId}/status")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @Parameter(description = "확인할 대상 유저 ID") @PathVariable Long targetId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(followService.isFollowing(currentUserId, targetId)));
    }
}

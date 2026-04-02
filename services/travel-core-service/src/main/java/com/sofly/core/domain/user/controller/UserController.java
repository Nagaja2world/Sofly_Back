package com.sofly.core.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofly.core.domain.user.dto.UserProfileResponse;
import com.sofly.core.domain.user.dto.UserProfileUpdateRequest;
import com.sofly.core.domain.user.service.UserService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "UserProfile", description = "프로필")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me/profile
     * 내 프로필 조회
     */
    @Operation(summary = "프로필 조회", description = "현재 로그인한 사용자의 프로필을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/users/me/profile
     * 내 프로필 등록 / 수정
     */
    @Operation(summary = "프로필 등록/수정", description = "현재 로그인한 사용자의 프로필을 등록하거나 수정합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록/수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값이 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @RequestBody @Valid UserProfileUpdateRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

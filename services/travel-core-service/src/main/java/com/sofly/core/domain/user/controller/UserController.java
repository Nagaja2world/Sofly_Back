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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me/profile
     * 내 프로필 조회
     */
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
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @RequestBody @Valid UserProfileUpdateRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

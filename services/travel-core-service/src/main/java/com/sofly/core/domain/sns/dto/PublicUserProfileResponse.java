package com.sofly.core.domain.sns.dto;

import com.sofly.core.global.response.PageResponse;

public record PublicUserProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        long followerCount,
        long followingCount,
        boolean isFollowing,
        PageResponse<PublicWorkspaceResponse> publicWorkspaces
) {}

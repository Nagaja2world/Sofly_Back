package com.sofly.core.domain.sns.dto;

import org.springframework.data.domain.Page;

public record PublicUserProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        long followerCount,
        long followingCount,
        boolean isFollowing,
        Page<PublicWorkspaceResponse> publicWorkspaces
) {}

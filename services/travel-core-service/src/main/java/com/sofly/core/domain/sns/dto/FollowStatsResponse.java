package com.sofly.core.domain.sns.dto;

public record FollowStatsResponse(
        long followerCount,
        long followingCount,
        boolean isFollowing
) {}

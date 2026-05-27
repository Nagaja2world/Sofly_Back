package com.sofly.core.domain.sns.dto;

import com.sofly.core.domain.user.entity.User;

public record AuthorInfo(
        Long userId,
        String nickname,
        String profileImageUrl
) {
    public static AuthorInfo from(User user) {
        return new AuthorInfo(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }
}

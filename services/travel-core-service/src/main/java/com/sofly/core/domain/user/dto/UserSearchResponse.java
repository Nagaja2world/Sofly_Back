package com.sofly.core.domain.user.dto;

import com.sofly.core.domain.user.entity.User;

public record UserSearchResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl
) {
    public static UserSearchResponse from(User user) {
        return new UserSearchResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}

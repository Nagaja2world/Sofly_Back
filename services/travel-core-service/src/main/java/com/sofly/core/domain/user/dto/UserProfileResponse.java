package com.sofly.core.domain.user.dto;

import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.entity.User.AgeGroup;
import com.sofly.core.domain.user.entity.User.CompanionType;
import com.sofly.core.domain.user.entity.User.TravelTheme;

import java.util.List;

/**
 * GET / PUT /api/users/me/profile 응답 DTO
 */
public record UserProfileResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,

        // 여행 성향
        AgeGroup ageGroup,
        String city,
        CompanionType preferCompanionType,
        Integer budgetMin,
        Integer budgetMax,
        List<TravelTheme> preferThemes,
        List<String> preferCities,

        // 프로필 완성 여부 (AI 연동 시 활용)
        boolean profileCompleted
) {
    public static UserProfileResponse from(User user) {
        boolean completed = user.getAgeGroup() != null
                && user.getPreferCompanionType() != null
                && user.getBudgetMin() != null
                && user.getBudgetMax() != null
                && !user.getPreferThemes().isEmpty();

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getAgeGroup(),
                user.getCity(),
                user.getPreferCompanionType(),
                user.getBudgetMin(),
                user.getBudgetMax(),
                List.copyOf(user.getPreferThemes()),
                List.copyOf(user.getPreferCities()),
                completed
        );
    }
}

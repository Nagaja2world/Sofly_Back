package com.sofly.core.domain.user.dto;

import com.sofly.core.domain.user.entity.User.AgeGroup;
import com.sofly.core.domain.user.entity.User.CompanionType;
import com.sofly.core.domain.user.entity.User.TravelTheme;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * PUT /api/users/me/profile 요청 DTO
 */
public record UserProfileUpdateRequest(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
        String nickname,

        String city,

        @NotNull(message = "연령대는 필수입니다.")
        AgeGroup ageGroup,

        @NotNull(message = "선호 동행 타입은 필수입니다.")
        CompanionType preferCompanionType,

        @PositiveOrZero(message = "최소 예산은 0 이상이어야 합니다.")
        Integer budgetMin,

        @PositiveOrZero(message = "최대 예산은 0 이상이어야 합니다.")
        Integer budgetMax,

        @Size(max = 5, message = "선호 테마는 최대 5개까지 선택 가능합니다.")
        List<TravelTheme> preferThemes,

        @Size(max = 10, message = "선호 도시는 최대 10개까지 선택 가능합니다.")
        List<String> preferCities
) {}

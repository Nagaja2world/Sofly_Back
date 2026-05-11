package com.sofly.core.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.sofly.core.domain.user.code.UserErrorCode;
import com.sofly.core.domain.user.dto.UserProfileResponse;
import com.sofly.core.domain.user.dto.UserProfileUpdateRequest;
import com.sofly.core.domain.user.dto.UserSearchResponse;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.exception.UserException;
import com.sofly.core.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * GET /api/users/me/profile
     * 현재 로그인한 사용자의 프로필 조회
     */
    public UserProfileResponse getMyProfile(Long userId) {
        User user = findUserById(userId);
        return UserProfileResponse.from(user);
    }

    /**
     * PUT /api/users/me/profile
     * 프로필 등록 / 수정 (기본 정보 + 여행 성향)
     */
    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UserProfileUpdateRequest request) {
        User user = findUserById(userId);

        // 예산 범위 검증
        validateBudgetRange(request.budgetMin(), request.budgetMax());

        // null이면 기존 값 유지
        user.updateProfile(
                request.nickname() != null ? request.nickname() : user.getNickname(),
                request.city() != null ? request.city() : user.getCity(),
                request.ageGroup() != null ? request.ageGroup() : user.getAgeGroup(),
                request.preferCompanionType() != null ? request.preferCompanionType() : user.getPreferCompanionType(),
                request.budgetMin() != null ? request.budgetMin() : user.getBudgetMin(),
                request.budgetMax() != null ? request.budgetMax() : user.getBudgetMax()
        );

        // 선호 테마 (다중) 업데이트
        if (request.preferThemes() != null) {
            user.updatePreferThemes(request.preferThemes());
        }

        // 선호 도시 태그 (다중) 업데이트
        if (request.preferCities() != null) {
            user.updatePreferCities(request.preferCities());
        }

        return UserProfileResponse.from(user);
    }

    /**
     * GET /api/users/search?email=...
     * 이메일 prefix로 사용자 검색 (자동완성 드롭다운용)
     */
    public List<UserSearchResponse> searchByEmail(String emailPrefix) {
        return userRepository.findTop10ByEmailStartingWithIgnoreCase(emailPrefix).stream()
                .map(UserSearchResponse::from)
                .toList();
    }

    // ── AI 일정 생성 연동용 ───────────────────────────────────

    /**
     * AI 일정 생성 시 사용자 프로필 정보를 기본 조건으로 제공
     * (다른 서비스에서 호출)
     */
    public UserProfileResponse getProfileForAiSchedule(Long userId) {
        return getMyProfile(userId);
    }

    // ── 내부 헬퍼 ───────────────────────────────────────────

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateBudgetRange(Integer budgetMin, Integer budgetMax) {
        if (budgetMin != null && budgetMax != null && budgetMin > budgetMax) {
            throw new UserException(UserErrorCode.INVALID_BUDGET_RANGE);
        }
    }
}

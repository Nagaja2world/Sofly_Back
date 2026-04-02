package com.sofly.core.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofly.core.domain.user.code.UserErrorCode;
import com.sofly.core.domain.user.dto.UserProfileResponse;
import com.sofly.core.domain.user.dto.UserProfileUpdateRequest;
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

        // 기본 정보 + 여행 성향 업데이트
        user.updateProfile(
                request.nickname(),
                request.city(),
                request.ageGroup(),
                request.preferCompanionType(),
                request.budgetMin(),
                request.budgetMax()
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
}

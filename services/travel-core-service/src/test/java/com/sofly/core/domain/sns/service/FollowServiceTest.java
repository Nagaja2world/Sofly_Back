package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.sns.code.SnsErrorCode;
import com.sofly.core.domain.sns.entity.UserFollow;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.sns.repository.UserFollowRepository;
import com.sofly.core.domain.user.code.UserErrorCode;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.exception.UserException;
import com.sofly.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock UserFollowRepository userFollowRepository;
    @Mock UserRepository userRepository;

    @InjectMocks FollowService followService;

    private User stubUser(Long id) {
        return User.builder().id(id).nickname("nick" + id).email(id + "@b.com")
                .provider(User.Provider.GOOGLE).providerId("p" + id).build();
    }

    @Test
    @DisplayName("자기 자신을 팔로우하면 CANNOT_FOLLOW_SELF 예외 발생")
    void follow_self_throwsSnsException() {
        assertThatThrownBy(() -> followService.follow(1L, 1L))
                .isInstanceOf(SnsException.class)
                .satisfies(e -> assertThat(((SnsException) e).getErrorCode())
                        .isEqualTo(SnsErrorCode.CANNOT_FOLLOW_SELF));
        verify(userFollowRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 팔로우 중이면 ALREADY_FOLLOWING 예외 발생")
    void follow_whenAlreadyFollowing_throwsSnsException() {
        given(userFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> followService.follow(1L, 2L))
                .isInstanceOf(SnsException.class)
                .satisfies(e -> assertThat(((SnsException) e).getErrorCode())
                        .isEqualTo(SnsErrorCode.ALREADY_FOLLOWING));
    }

    @Test
    @DisplayName("팔로우 대상 유저가 없으면 UserException 발생 (EntityNotFoundException 아님)")
    void follow_whenTargetUserNotFound_throwsUserException() {
        given(userFollowRepository.existsByFollowerIdAndFollowingId(1L, 999L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(stubUser(1L)));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> followService.follow(1L, 999L))
                .isInstanceOf(UserException.class)
                .satisfies(e -> assertThat(((UserException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("follow 정상 흐름 — UserFollow 저장")
    void follow_happy_path() {
        given(userFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(stubUser(1L)));
        given(userRepository.findById(2L)).willReturn(Optional.of(stubUser(2L)));

        followService.follow(1L, 2L);

        verify(userFollowRepository).save(any(UserFollow.class));
    }

    @Test
    @DisplayName("팔로우 관계가 없는데 unfollow 시 FOLLOW_NOT_FOUND 예외 발생")
    void unfollow_whenNotFollowing_throwsSnsException() {
        given(userFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> followService.unfollow(1L, 2L))
                .isInstanceOf(SnsException.class)
                .satisfies(e -> assertThat(((SnsException) e).getErrorCode())
                        .isEqualTo(SnsErrorCode.FOLLOW_NOT_FOUND));
    }

    @Test
    @DisplayName("unfollow 정상 흐름 — UserFollow 삭제")
    void unfollow_happy_path() {
        given(userFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(true);

        followService.unfollow(1L, 2L);

        verify(userFollowRepository).deleteByFollowerIdAndFollowingId(1L, 2L);
    }
}

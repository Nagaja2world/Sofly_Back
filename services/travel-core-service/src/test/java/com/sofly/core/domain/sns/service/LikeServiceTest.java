package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.sns.code.SnsErrorCode;
import com.sofly.core.domain.sns.entity.WorkspaceLike;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.sns.repository.WorkspaceLikeRepository;
import com.sofly.core.domain.user.code.UserErrorCode;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.exception.UserException;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.entity.WorkspaceVisibility;
import com.sofly.core.domain.workspace.exception.WorkspaceException;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import com.sofly.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock WorkspaceLikeRepository workspaceLikeRepository;
    @Mock UserRepository userRepository;
    @Mock WorkspaceRepository workspaceRepository;

    @InjectMocks LikeService likeService;

    private User stubUser(Long id) {
        return User.builder().id(id).nickname("nick").email("a@b.com")
                .provider(User.Provider.GOOGLE).providerId("p").build();
    }

    private Workspace stubWorkspace(Long id) {
        return Workspace.builder().id(id).title("t").destination("d")
                .countryCode("JP").startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .owner(stubUser(99L)).visibility(WorkspaceVisibility.PUBLIC).build();
    }

    @Test
    @DisplayName("이미 좋아요된 워크스페이스에 like() 호출 시 ALREADY_LIKED 예외 발생")
    void like_whenAlreadyLiked_throwsSnsException() {
        given(workspaceLikeRepository.existsByWorkspaceIdAndUserId(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> likeService.like(1L, 1L))
                .isInstanceOf(SnsException.class)
                .satisfies(e -> assertThat(((SnsException) e).getErrorCode())
                        .isEqualTo(SnsErrorCode.ALREADY_LIKED));

        verify(workspaceLikeRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 유저로 like() 호출 시 UserException 발생 (EntityNotFoundException 아님)")
    void like_whenUserNotFound_throwsUserException() {
        given(workspaceLikeRepository.existsByWorkspaceIdAndUserId(1L, 999L)).willReturn(false);
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.like(999L, 1L))
                .isInstanceOf(UserException.class)
                .satisfies(e -> assertThat(((UserException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("like() 정상 호출 시 WorkspaceLike 저장")
    void like_happy_path() {
        User user = stubUser(1L);
        Workspace ws = stubWorkspace(1L);

        given(workspaceLikeRepository.existsByWorkspaceIdAndUserId(1L, 1L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(workspaceRepository.findById(1L)).willReturn(Optional.of(ws));

        likeService.like(1L, 1L);

        verify(workspaceLikeRepository).save(any(WorkspaceLike.class));
    }

    @Test
    @DisplayName("unlike() 시 좋아요가 없으면 LIKE_NOT_FOUND 예외 발생")
    void unlike_whenNotLiked_throwsSnsException() {
        User user = stubUser(1L);
        Workspace ws = stubWorkspace(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(workspaceRepository.findById(1L)).willReturn(Optional.of(ws));
        given(workspaceLikeRepository.findByWorkspaceIdAndUserId(1L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.unlike(1L, 1L))
                .isInstanceOf(SnsException.class)
                .satisfies(e -> assertThat(((SnsException) e).getErrorCode())
                        .isEqualTo(SnsErrorCode.LIKE_NOT_FOUND));
    }
}

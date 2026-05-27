package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.sns.code.SnsErrorCode;
import com.sofly.core.domain.sns.entity.WorkspaceComment;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.sns.repository.UserFollowRepository;
import com.sofly.core.domain.sns.repository.WorkspaceCommentRepository;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.entity.WorkspaceVisibility;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock WorkspaceCommentRepository commentRepository;
    @Mock UserRepository userRepository;
    @Mock WorkspaceRepository workspaceRepository;
    @Mock UserFollowRepository userFollowRepository;

    @InjectMocks CommentService commentService;

    private User stubUser(Long id) {
        return User.builder().id(id).nickname("nick").email("a@b.com")
                .provider(User.Provider.GOOGLE).providerId("p").build();
    }

    private Workspace stubWorkspace(Long id, WorkspaceVisibility visibility) {
        return Workspace.builder().id(id).title("t").destination("d")
                .countryCode("JP").startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .owner(stubUser(99L)).visibility(visibility).build();
    }

    private WorkspaceComment stubComment(Long commentId, Long authorId) {
        return WorkspaceComment.builder().id(commentId)
                .author(stubUser(authorId))
                .workspace(stubWorkspace(1L, WorkspaceVisibility.PUBLIC))
                .content("내용").build();
    }

    @Test
    @DisplayName("댓글 작성 — WorkspaceComment 저장")
    void createComment_savesComment() {
        given(userRepository.findById(1L)).willReturn(Optional.of(stubUser(1L)));
        given(workspaceRepository.findById(1L)).willReturn(Optional.of(stubWorkspace(1L, WorkspaceVisibility.PUBLIC)));
        given(commentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        commentService.createComment(1L, 1L, "안녕하세요");

        verify(commentRepository).save(any(WorkspaceComment.class));
    }

    @Test
    @DisplayName("PRIVATE 워크스페이스에 댓글 작성 시 WORKSPACE_NOT_PUBLIC 발생")
    void createComment_onPrivateWorkspace_throwsWorkspaceNotPublic() {
        given(userRepository.findById(1L)).willReturn(Optional.of(stubUser(1L)));
        given(workspaceRepository.findById(1L)).willReturn(Optional.of(stubWorkspace(1L, WorkspaceVisibility.PRIVATE)));

        assertThatThrownBy(() -> commentService.createComment(1L, 1L, "안녕하세요"))
                .isInstanceOf(SnsException.class)
                .satisfies(e -> assertThat(((SnsException) e).getErrorCode())
                        .isEqualTo(SnsErrorCode.WORKSPACE_NOT_PUBLIC));
    }

    @Test
    @DisplayName("PRIVATE 워크스페이스 댓글 목록 조회 시 WORKSPACE_NOT_PUBLIC 발생")
    void getComments_onPrivateWorkspace_throwsWorkspaceNotPublic() {
        given(workspaceRepository.findById(1L)).willReturn(Optional.of(stubWorkspace(1L, WorkspaceVisibility.PRIVATE)));

        assertThatThrownBy(() -> commentService.getComments(1L, null, PageRequest.of(0, 20)))
                .isInstanceOf(SnsException.class)
                .satisfies(e -> assertThat(((SnsException) e).getErrorCode())
                        .isEqualTo(SnsErrorCode.WORKSPACE_NOT_PUBLIC));
    }

    @Test
    @DisplayName("FOLLOWERS_ONLY 워크스페이스에 팔로워가 아닌 경우 댓글 작성 시 WORKSPACE_NOT_PUBLIC 발생")
    void createComment_onFollowersOnlyWorkspace_whenNotFollower_throwsWorkspaceNotPublic() {
        given(userRepository.findById(1L)).willReturn(Optional.of(stubUser(1L)));
        given(workspaceRepository.findById(1L)).willReturn(Optional.of(stubWorkspace(1L, WorkspaceVisibility.FOLLOWERS_ONLY)));
        given(userFollowRepository.existsByFollowerIdAndFollowingId(1L, 99L)).willReturn(false);

        assertThatThrownBy(() -> commentService.createComment(1L, 1L, "안녕하세요"))
                .isInstanceOf(SnsException.class)
                .satisfies(e -> assertThat(((SnsException) e).getErrorCode())
                        .isEqualTo(SnsErrorCode.WORKSPACE_NOT_PUBLIC));
    }

    @Test
    @DisplayName("본인 댓글이 아닌 경우 수정 시 COMMENT_FORBIDDEN 발생")
    void updateComment_whenNotAuthor_throwsForbidden() {
        WorkspaceComment comment = stubComment(1L, 2L); // author=2, requester=1
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(1L, 1L, "수정"))
                .isInstanceOf(SnsException.class)
                .satisfies(e -> assertThat(((SnsException) e).getErrorCode())
                        .isEqualTo(SnsErrorCode.COMMENT_FORBIDDEN));
    }

    @Test
    @DisplayName("댓글이 없는 경우 수정 시 COMMENT_NOT_FOUND 발생")
    void updateComment_whenNotFound_throwsNotFound() {
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(1L, 999L, "수정"))
                .isInstanceOf(SnsException.class)
                .satisfies(e -> assertThat(((SnsException) e).getErrorCode())
                        .isEqualTo(SnsErrorCode.COMMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("본인 댓글이 아닌 경우 삭제 시 COMMENT_FORBIDDEN 발생")
    void deleteComment_whenNotAuthor_throwsForbidden() {
        WorkspaceComment comment = stubComment(1L, 2L);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(1L, 1L))
                .isInstanceOf(SnsException.class)
                .satisfies(e -> assertThat(((SnsException) e).getErrorCode())
                        .isEqualTo(SnsErrorCode.COMMENT_FORBIDDEN));
    }
}

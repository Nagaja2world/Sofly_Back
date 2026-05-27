package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.sns.dto.PublicWorkspaceResponse;
import com.sofly.core.domain.sns.repository.UserFollowRepository;
import com.sofly.core.domain.sns.repository.WorkspaceCommentRepository;
import com.sofly.core.domain.sns.repository.WorkspaceLikeRepository;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.entity.WorkspaceVisibility;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock WorkspaceRepository workspaceRepository;
    @Mock UserFollowRepository userFollowRepository;
    @Mock WorkspaceLikeRepository workspaceLikeRepository;
    @Mock WorkspaceCommentRepository workspaceCommentRepository;

    @InjectMocks FeedService feedService;

    private User stubUser(Long id) {
        return User.builder().id(id).nickname("nick").email("a@b.com")
                .provider(User.Provider.GOOGLE).providerId("p").build();
    }

    private Workspace stubWorkspace(Long id) {
        return Workspace.builder()
                .id(id).title("t").destination("d").countryCode("JP")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(3))
                .owner(stubUser(99L)).visibility(WorkspaceVisibility.PUBLIC).build();
    }

    @Test
    @DisplayName("팔로잉이 없어도 공개 워크스페이스로 피드를 구성한다")
    void getFeed_withNoFollowings_returnsPublicWorkspaces() {
        Workspace ws = stubWorkspace(1L);
        PageRequest pageable = PageRequest.of(0, 20);

        given(userFollowRepository.findFollowingIdsByFollowerId(1L)).willReturn(List.of());
        given(workspaceRepository.findAllPublic(any()))
                .willReturn(new PageImpl<>(List.of(ws)));
        given(workspaceLikeRepository.countByWorkspaceIds(anyList())).willReturn(List.of());
        given(workspaceCommentRepository.countByWorkspaceIds(anyList())).willReturn(List.of());
        given(workspaceLikeRepository.findLikedWorkspaceIdsByUserId(anyLong(), anyList()))
                .willReturn(List.of());

        Page<PublicWorkspaceResponse> result = feedService.getFeed(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("좋아요 많은 워크스페이스가 상위에 정렬된다 (score = likeCount×3)")
    void getFeed_sortsByScore_highLikesFirst() {
        Workspace lowLike = stubWorkspace(1L);
        Workspace highLike = stubWorkspace(2L);
        PageRequest pageable = PageRequest.of(0, 20);

        given(userFollowRepository.findFollowingIdsByFollowerId(1L)).willReturn(List.of());
        given(workspaceRepository.findAllPublic(any()))
                .willReturn(new PageImpl<>(List.of(lowLike, highLike)));
        given(workspaceLikeRepository.countByWorkspaceIds(anyList()))
                .willReturn(Collections.singletonList(new Object[]{2L, 10L})); // highLike(id=2)만 10개
        given(workspaceCommentRepository.countByWorkspaceIds(anyList())).willReturn(List.of());
        given(workspaceLikeRepository.findLikedWorkspaceIdsByUserId(anyLong(), anyList()))
                .willReturn(List.of());

        Page<PublicWorkspaceResponse> result = feedService.getFeed(1L, pageable);

        assertThat(result.getContent().getFirst().getId()).isEqualTo(2L);
    }
}

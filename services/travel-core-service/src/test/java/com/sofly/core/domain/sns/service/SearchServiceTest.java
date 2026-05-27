package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.schedule.repository.ScheduleRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock WorkspaceRepository workspaceRepository;
    @Mock WorkspaceLikeRepository workspaceLikeRepository;
    @Mock WorkspaceCommentRepository workspaceCommentRepository;
    @Mock ScheduleRepository scheduleRepository;

    @InjectMocks SearchService searchService;

    private User stubUser(Long id) {
        return User.builder().id(id).nickname("nick").email("a@b.com")
                .provider(User.Provider.GOOGLE).providerId("p").build();
    }

    private Workspace stubWorkspace(Long id) {
        return Workspace.builder()
                .id(id).title("Tokyo").destination("Japan").countryCode("JP")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(3))
                .owner(stubUser(99L)).visibility(WorkspaceVisibility.PUBLIC).build();
    }

    @Test
    @DisplayName("검색어가 없으면 키워드 없는 공개 워크스페이스 쿼리를 사용한다")
    void search_withoutKeyword_usesPublicSearchOnly() {
        PageRequest pageable = PageRequest.of(0, 20);
        Workspace workspace = stubWorkspace(1L);

        given(workspaceRepository.searchPublic(null, pageable))
                .willReturn(new PageImpl<>(List.of(workspace)));
        given(workspaceLikeRepository.countByWorkspaceIds(anyList())).willReturn(List.of());
        given(workspaceCommentRepository.countByWorkspaceIds(anyList())).willReturn(List.of());

        Page<?> result = searchService.search(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(workspaceRepository).searchPublic(null, pageable);
        verify(workspaceRepository, never()).searchPublicByKeyword(null, null, pageable);
    }

    @Test
    @DisplayName("검색어가 있으면 키워드 공개 워크스페이스 쿼리를 사용한다 (와일드카드 이스케이프 적용)")
    void search_withKeyword_usesKeywordSearch() {
        PageRequest pageable = PageRequest.of(0, 20);
        Workspace workspace = stubWorkspace(1L);

        // 와일드카드 이스케이프 후 "Tokyo" → "Tokyo" (특수문자 없으면 그대로)
        given(workspaceRepository.searchPublicByKeyword("JP", "Tokyo", pageable))
                .willReturn(new PageImpl<>(List.of(workspace)));
        given(workspaceLikeRepository.countByWorkspaceIds(anyList())).willReturn(List.of());
        given(workspaceCommentRepository.countByWorkspaceIds(anyList())).willReturn(List.of());

        Page<?> result = searchService.search(" jp ", " Tokyo ", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(workspaceRepository).searchPublicByKeyword("JP", "Tokyo", pageable);
        verify(workspaceRepository, never()).searchPublic("JP", pageable);
    }

    @Test
    @DisplayName("검색어에 % 특수문자가 포함되면 이스케이프 처리된다")
    void search_withWildcardKeyword_escapesSpecialChars() {
        PageRequest pageable = PageRequest.of(0, 20);
        // "100%_trip" → 이스케이프 후 "100\%\_trip"
        String escaped = "100\\%\\_trip";

        given(workspaceRepository.searchPublicByKeyword(null, escaped, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<?> result = searchService.search(null, "100%_trip", null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(workspaceRepository).searchPublicByKeyword(null, escaped, pageable);
    }

    @Test
    @DisplayName("요청 페이지가 비어도 전체 개수 메타데이터를 유지한다")
    void search_emptyPage_keepsTotalElements() {
        PageRequest pageable = PageRequest.of(1, 2);

        given(workspaceRepository.searchPublic(null, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 2));

        Page<?> result = searchService.search(null, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }
}

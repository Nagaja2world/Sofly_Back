package com.sofly.core.domain.sns.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofly.core.domain.sns.service.WorkspaceImportService;
import com.sofly.core.domain.workspace.dto.response.WorkspaceResponse;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "SNS - 워크스페이스 가져오기", description = "다른 유저의 공개 워크스페이스를 내 워크스페이스로 복제")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sns/workspaces")
public class WorkspaceImportController {

    private final WorkspaceImportService workspaceImportService;

    @Operation(
        summary = "워크스페이스 가져오기",
        description = """
            다른 유저의 PUBLIC 워크스페이스를 복제하여 내 새 워크스페이스로 만듭니다.

            **복제되는 것**
            - 워크스페이스 기본 정보 (제목, 목적지, 국가코드, 날짜, 인원, 커버 이미지)
            - 최신 버전 일정 (Schedule + ScheduleItem 전체)

            **복제되지 않는 것**
            - 멤버 (호출한 유저가 OWNER로 단독 등록)
            - 항공편, 저장된 장소, 앨범, 여행로그
            - 초대 코드

            새로 만들어진 워크스페이스의 공개 범위는 기본값(PRIVATE)으로 설정됩니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "가져오기 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PUBLIC 워크스페이스가 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @PostMapping("/{workspaceId}/import")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> importWorkspace(
            @PathVariable Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                workspaceImportService.importWorkspace(workspaceId, userId)));
    }
}

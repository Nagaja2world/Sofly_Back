package com.sofly.core.domain.sns.controller;

import com.sofly.core.domain.sns.dto.PublicWorkspaceResponse;
import com.sofly.core.domain.sns.service.SearchService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.response.PageResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Search", description = "공개 워크스페이스 검색")
@RestController
@RequestMapping("/api/sns/workspaces")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "공개 워크스페이스 검색",
               description = "미인증 접근 가능. page는 0부터 시작합니다. countryCode(ISO 3166-1 alpha-2), keyword(목적지/제목) 필터.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<PublicWorkspaceResponse>>> search(
            @Parameter(description = "국가 코드 (예: JP)") @RequestParam(required = false) String countryCode,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long viewerId = SecurityUtils.tryGetCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(searchService.search(countryCode, keyword, viewerId, pageable))));
    }
}

package com.sofly.core.domain.sns.controller;

import com.sofly.core.domain.sns.dto.PublicWorkspaceResponse;
import com.sofly.core.domain.sns.dto.ScheduleSummary;
import com.sofly.core.domain.sns.dto.TrendingDestinationResponse;
import com.sofly.core.domain.sns.service.SearchService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.response.PageResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import java.util.List;
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

    @Operation(summary = "요즘 뜨는 여행지 Top 10",
               description = "미인증 접근 가능. 최근 30일 PUBLIC 워크스페이스 destination 집계 기준. 데이터 부족 시 전체 기간 사용.")
    @GetMapping("/trending-destinations")
    public ResponseEntity<ApiResponse<List<TrendingDestinationResponse>>> getTrendingDestinations() {
        return ResponseEntity.ok(ApiResponse.success(searchService.getTrendingDestinations()));
    }

    @Operation(summary = "공개 워크스페이스 최신 일정 조회 (on-demand)",
               description = "미인증 접근 가능. 피드/검색 목록에서 특정 워크스페이스의 일정 상세가 필요할 때 호출합니다.")
    @GetMapping("/{workspaceId}/latest-schedule")
    public ResponseEntity<ApiResponse<ScheduleSummary>> getLatestSchedule(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId) {
        return ResponseEntity.ok(ApiResponse.success(searchService.getLatestSchedule(workspaceId)));
    }
}

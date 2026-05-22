package com.sofly.core.domain.sns.controller;

import com.sofly.core.domain.sns.dto.PublicWorkspaceResponse;
import com.sofly.core.domain.sns.service.FeedService;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feed", description = "알고리즘 피드")
@RestController
@RequestMapping("/api/sns/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "알고리즘 피드 조회",
               description = "인증 필수. 팔로잉+최근 공개 워크스페이스를 score(좋아요×3+댓글×2+최근생성+10) 순 반환.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PublicWorkspaceResponse>>> getFeed(
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(feedService.getFeed(userId, pageable)));
    }
}

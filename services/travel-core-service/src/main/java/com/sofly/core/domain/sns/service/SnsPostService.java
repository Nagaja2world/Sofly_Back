package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.album.service.S3Service;
import com.sofly.core.domain.sns.code.SnsErrorCode;
import com.sofly.core.domain.sns.dto.SnsPostFeedResponse;
import com.sofly.core.domain.sns.dto.SnsPostResponse;
import com.sofly.core.domain.sns.dto.SnsPostUpdateRequest;
import com.sofly.core.domain.sns.entity.SnsPost;
import com.sofly.core.domain.sns.entity.SnsPostImage;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.sns.repository.SnsPostRepository;
import com.sofly.core.domain.sns.repository.UserFollowRepository;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.repository.WorkspaceMemberRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SnsPostService {

    private static final int MAX_IMAGES = 10;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic"
    );

    private final SnsPostRepository snsPostRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final S3Service s3Service;

    /** SNS 카드 생성 (워크스페이스당 1개) */
    @Transactional
    public SnsPostResponse createPost(Long workspaceId, Long userId,
                                      List<MultipartFile> files,
                                      String content,
                                      SnsPost.Visibility visibility) {
        validateMember(workspaceId, userId);

        if (snsPostRepository.existsByWorkspaceId(workspaceId)) {
            throw new SnsException(SnsErrorCode.SNS_POST_ALREADY_EXISTS);
        }

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new SoflyException(ErrorCode.USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_NOT_FOUND));

        if (files != null && !files.isEmpty()) {
            validateFiles(files);
        }

        SnsPost post = SnsPost.builder()
                .workspace(workspace)
                .author(author)
                .content(content)
                .visibility(visibility)
                .build();
        snsPostRepository.save(post);

        uploadImages(post, workspaceId, files, 0);

        return SnsPostResponse.from(post);
    }

    /** 워크스페이스 SNS 카드 단건 조회 (전체 이미지) — 공개범위에 따라 접근 제어 */
    public SnsPostResponse getPost(Long workspaceId, Long userId) {
        SnsPost post = snsPostRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new SnsException(SnsErrorCode.SNS_POST_NOT_FOUND));
        checkVisibilityAccess(post, userId);
        return SnsPostResponse.from(post);
    }

    /**
     * SNS 카드 수정
     * - keepImageIds 제공 시: 해당 ID 이미지 유지(순서 재정렬) + 나머지 삭제 + newFiles 추가
     * - keepImageIds 미제공 + newFiles 있으면: 기존 이미지 전체 교체
     * - 둘 다 없으면: 텍스트·공개범위만 수정
     */
    @Transactional
    public SnsPostResponse updatePost(Long workspaceId, Long userId,
                                      List<MultipartFile> newFiles,
                                      List<Long> keepImageIds,
                                      SnsPostUpdateRequest request) {
        validateMember(workspaceId, userId);
        SnsPost post = snsPostRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new SnsException(SnsErrorCode.SNS_POST_NOT_FOUND));

        if (!post.isAuthor(userId)) {
            throw new SnsException(SnsErrorCode.SNS_POST_FORBIDDEN);
        }

        post.update(request.content(), request.visibility());

        boolean hasKeepIds = keepImageIds != null;
        boolean hasNewFiles = newFiles != null && !newFiles.isEmpty();

        if (hasKeepIds) {
            // 부분 업데이트: keepImageIds 유지 + 나머지 삭제 + 새 파일 추가
            if (hasNewFiles) validateFiles(newFiles);

            int totalCount = keepImageIds.size() + (hasNewFiles ? newFiles.size() : 0);
            if (totalCount > MAX_IMAGES) {
                throw new SoflyException(ErrorCode.TOO_MANY_FILES);
            }

            // keepImageIds에 없는 이미지 S3 삭제 후 컬렉션에서 제거
            List<SnsPostImage> toDelete = post.getImages().stream()
                    .filter(img -> !keepImageIds.contains(img.getId()))
                    .toList();
            toDelete.forEach(img -> {
                try { s3Service.deleteObject(img.getS3Key()); } catch (Exception e) {
                    log.warn("SNS 이미지 S3 삭제 실패: {}", img.getS3Key());
                }
                post.removeImage(img);
            });

            // 유지되는 이미지 orderIndex를 keepImageIds 순서로 재정렬
            Map<Long, SnsPostImage> imageMap = post.getImages().stream()
                    .collect(java.util.stream.Collectors.toMap(SnsPostImage::getId, img -> img));
            for (int i = 0; i < keepImageIds.size(); i++) {
                SnsPostImage img = imageMap.get(keepImageIds.get(i));
                if (img != null) img.updateOrderIndex(i);
            }

            // 새 파일 업로드 (유지 이미지 다음 순서로)
            if (hasNewFiles) {
                uploadImages(post, workspaceId, newFiles, keepImageIds.size());
            }

        } else if (hasNewFiles) {
            // 전체 교체: 기존 이미지 전부 삭제 후 새 파일로 교체
            validateFiles(newFiles);
            post.getImages().forEach(img -> {
                try { s3Service.deleteObject(img.getS3Key()); } catch (Exception e) {
                    log.warn("SNS 이미지 S3 삭제 실패: {}", img.getS3Key());
                }
            });
            post.clearImages();
            uploadImages(post, workspaceId, newFiles, 0);
        }

        return SnsPostResponse.from(post);
    }

    /** SNS 카드 삭제 */
    @Transactional
    public void deletePost(Long workspaceId, Long userId) {
        validateMember(workspaceId, userId);
        SnsPost post = snsPostRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new SnsException(SnsErrorCode.SNS_POST_NOT_FOUND));

        if (!post.isAuthor(userId)) {
            throw new SnsException(SnsErrorCode.SNS_POST_FORBIDDEN);
        }

        post.getImages().forEach(img -> {
            try { s3Service.deleteObject(img.getS3Key()); } catch (Exception e) {
                log.warn("SNS 이미지 S3 삭제 실패: {}", img.getS3Key());
            }
        });
        snsPostRepository.delete(post);
    }

    /** SNS 피드: PUBLIC(전체) + FOLLOWERS_ONLY(팔로잉 대상만) */
    public Page<SnsPostFeedResponse> getFeed(Long userId, Pageable pageable) {
        List<Long> followingIds = userFollowRepository.findFollowingIdsByFollowerId(userId);
        Page<SnsPost> page;
        if (followingIds.isEmpty()) {
            page = snsPostRepository.findPublicForFeed(SnsPost.Visibility.PUBLIC, pageable);
        } else {
            page = snsPostRepository.findForFeed(
                    SnsPost.Visibility.PUBLIC,
                    SnsPost.Visibility.FOLLOWERS_ONLY,
                    followingIds,
                    pageable);
        }
        return page.map(SnsPostFeedResponse::from);
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────

    private void uploadImages(SnsPost post, Long workspaceId, List<MultipartFile> files, int startIndex) {
        if (files == null || files.isEmpty()) return;
        List<String> uploadedKeys = new ArrayList<>();
        try {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String ext = resolveExtension(file);
                String s3Key = String.format("sns-posts/%d/%s.%s", workspaceId, UUID.randomUUID(), ext);
                s3Service.uploadFile(file, s3Key, true);
                uploadedKeys.add(s3Key);
                post.addImage(SnsPostImage.of(post, s3Key, s3Service.buildObjectUrl(s3Key), startIndex + i));
            }
        } catch (RuntimeException e) {
            for (String key : uploadedKeys) {
                try { s3Service.deleteObject(key); } catch (Exception ex) {
                    log.warn("S3 rollback 실패: {}", key);
                }
            }
            throw e;
        }
    }

    /**
     * SNS 카드 공개범위 접근 제어
     * PUBLIC       → 누구나
     * FOLLOWERS_ONLY → 작성자 본인 + 팔로워
     * PRIVATE      → 작성자 본인만
     */
    private void checkVisibilityAccess(SnsPost post, Long userId) {
        if (post.isAuthor(userId)) return;

        switch (post.getVisibility()) {
            case PUBLIC -> { /* 누구나 접근 가능 */ }
            case FOLLOWERS_ONLY -> {
                boolean follows = userFollowRepository.existsByFollowerIdAndFollowingId(
                        userId, post.getAuthor().getId());
                boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                        post.getWorkspace().getId(), userId);
                if (!follows && !isMember) throw new SnsException(SnsErrorCode.SNS_POST_FORBIDDEN);
            }
            case PRIVATE -> throw new SnsException(SnsErrorCode.SNS_POST_FORBIDDEN);
        }
    }

    private void validateMember(Long workspaceId, Long userId) {
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_ACCESS_DENIED));
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files.size() > MAX_IMAGES) {
            throw new SoflyException(ErrorCode.TOO_MANY_FILES);
        }
        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new SoflyException(ErrorCode.FILE_TOO_LARGE);
            }
            if (!ALLOWED_TYPES.contains(file.getContentType())) {
                throw new SoflyException(ErrorCode.INVALID_FILE_TYPE);
            }
        }
    }

    private String resolveExtension(MultipartFile file) {
        return switch (file.getContentType()) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            case "image/heic" -> "heic";
            default -> throw new SoflyException(ErrorCode.INVALID_FILE_TYPE);
        };
    }
}

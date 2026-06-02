package com.sofly.core.domain.sns.service;

import com.sofly.core.domain.album.entity.Photo;
import com.sofly.core.domain.album.repository.PhotoRepository;
import com.sofly.core.domain.album.service.S3Service;
import com.sofly.core.domain.sns.code.SnsErrorCode;
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
import com.sofly.core.domain.workspace.entity.WorkspaceVisibility;
import com.sofly.core.domain.workspace.repository.WorkspaceMemberRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final PhotoRepository photoRepository;
    private final S3Service s3Service;

    /** SNS 카드 생성 (워크스페이스당 1개) */
    @Transactional
    public SnsPostResponse createPost(Long workspaceId, Long userId,
                                      List<MultipartFile> files,
                                      List<Long> albumPhotoIds,
                                      String content) {
        validateMember(workspaceId, userId);

        if (snsPostRepository.existsByWorkspaceId(workspaceId)) {
            throw new SnsException(SnsErrorCode.SNS_POST_ALREADY_EXISTS);
        }

        boolean hasAlbumPhotos = albumPhotoIds != null && !albumPhotoIds.isEmpty();
        boolean hasFiles = files != null && !files.isEmpty();

        int totalCount = (hasAlbumPhotos ? albumPhotoIds.size() : 0) + (hasFiles ? files.size() : 0);
        if (totalCount > MAX_IMAGES) {
            throw new SoflyException(ErrorCode.TOO_MANY_FILES);
        }
        if (hasFiles) validateFiles(files);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new SoflyException(ErrorCode.USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_NOT_FOUND));

        SnsPost post = SnsPost.builder()
                .workspace(workspace)
                .author(author)
                .content(content)
                .build();
        snsPostRepository.save(post);

        // 순서: albumPhotos → 새 파일 업로드
        copyAlbumPhotos(post, workspaceId, albumPhotoIds, 0);
        int albumCount = hasAlbumPhotos ? albumPhotoIds.size() : 0;
        uploadImages(post, workspaceId, files, albumCount);

        return SnsPostResponse.from(post);
    }

    /** 워크스페이스 SNS 카드 단건 조회 — 워크스페이스 공개범위 기준으로 접근 제어 */
    public SnsPostResponse getPost(Long workspaceId, Long userId) {
        SnsPost post = snsPostRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new SnsException(SnsErrorCode.SNS_POST_NOT_FOUND));
        checkWorkspaceVisibilityAccess(workspaceId, userId);
        return SnsPostResponse.from(post);
    }

    /**
     * SNS 카드 수정
     * 순서: keepImageIds → albumPhotoIds → 새 파일 업로드
     * - keepImageIds 제공 시: 해당 ID 이미지 유지(순서 재정렬) + 나머지 삭제 + albumPhotos + newFiles 추가
     * - keepImageIds 미제공 + (albumPhotos || newFiles) 있으면: 기존 이미지 전체 교체
     * - 모두 없으면: 텍스트만 수정
     */
    @Transactional
    public SnsPostResponse updatePost(Long workspaceId, Long userId,
                                      List<MultipartFile> newFiles,
                                      List<Long> keepImageIds,
                                      List<Long> albumPhotoIds,
                                      SnsPostUpdateRequest request) {
        validateMember(workspaceId, userId);
        SnsPost post = snsPostRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new SnsException(SnsErrorCode.SNS_POST_NOT_FOUND));

        if (!post.isAuthor(userId) && !isWorkspaceOwner(workspaceId, userId)) {
            throw new SnsException(SnsErrorCode.SNS_POST_FORBIDDEN);
        }

        post.update(request.content());

        boolean hasKeepIds = keepImageIds != null;
        boolean hasNewFiles = newFiles != null && !newFiles.isEmpty();
        boolean hasAlbumPhotos = albumPhotoIds != null && !albumPhotoIds.isEmpty();

        if (hasKeepIds) {
            if (hasNewFiles) validateFiles(newFiles);

            int albumCount = hasAlbumPhotos ? albumPhotoIds.size() : 0;
            int totalCount = keepImageIds.size() + albumCount + (hasNewFiles ? newFiles.size() : 0);
            if (totalCount > MAX_IMAGES) {
                throw new SoflyException(ErrorCode.TOO_MANY_FILES);
            }

            List<SnsPostImage> toDelete = post.getImages().stream()
                    .filter(img -> !keepImageIds.contains(img.getId()))
                    .toList();
            toDelete.forEach(img -> {
                try { s3Service.deleteObject(img.getS3Key()); } catch (Exception e) {
                    log.warn("SNS 이미지 S3 삭제 실패: {}", img.getS3Key());
                }
                post.removeImage(img);
            });

            Map<Long, SnsPostImage> imageMap = post.getImages().stream()
                    .collect(Collectors.toMap(SnsPostImage::getId, img -> img));
            for (int i = 0; i < keepImageIds.size(); i++) {
                SnsPostImage img = imageMap.get(keepImageIds.get(i));
                if (img != null) img.updateOrderIndex(i);
            }

            // 순서: keepImageIds 다음부터 albumPhotos, 그 다음 새 파일
            copyAlbumPhotos(post, workspaceId, albumPhotoIds, keepImageIds.size());
            if (hasNewFiles) {
                uploadImages(post, workspaceId, newFiles, keepImageIds.size() + albumCount);
            }

        } else if (hasNewFiles || hasAlbumPhotos) {
            if (hasNewFiles) validateFiles(newFiles);

            int albumCount = hasAlbumPhotos ? albumPhotoIds.size() : 0;
            int totalCount = albumCount + (hasNewFiles ? newFiles.size() : 0);
            if (totalCount > MAX_IMAGES) {
                throw new SoflyException(ErrorCode.TOO_MANY_FILES);
            }

            post.getImages().forEach(img -> {
                try { s3Service.deleteObject(img.getS3Key()); } catch (Exception e) {
                    log.warn("SNS 이미지 S3 삭제 실패: {}", img.getS3Key());
                }
            });
            post.clearImages();

            // 순서: albumPhotos → 새 파일
            copyAlbumPhotos(post, workspaceId, albumPhotoIds, 0);
            uploadImages(post, workspaceId, newFiles, albumCount);
        }

        return SnsPostResponse.from(post);
    }

    /** SNS 카드 삭제 */
    @Transactional
    public void deletePost(Long workspaceId, Long userId) {
        validateMember(workspaceId, userId);
        SnsPost post = snsPostRepository.findByWorkspaceId(workspaceId)
                .orElseThrow(() -> new SnsException(SnsErrorCode.SNS_POST_NOT_FOUND));

        if (!post.isAuthor(userId) && !isWorkspaceOwner(workspaceId, userId)) {
            throw new SnsException(SnsErrorCode.SNS_POST_FORBIDDEN);
        }

        post.getImages().forEach(img -> {
            try { s3Service.deleteObject(img.getS3Key()); } catch (Exception e) {
                log.warn("SNS 이미지 S3 삭제 실패: {}", img.getS3Key());
            }
        });
        snsPostRepository.delete(post);
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
     * 앨범 사진을 SNS 포스트용 S3 경로로 복사하여 추가.
     * 앨범 원본과 독립적으로 관리되도록 복사 방식을 사용.
     */
    private void copyAlbumPhotos(SnsPost post, Long workspaceId, List<Long> albumPhotoIds, int startIndex) {
        if (albumPhotoIds == null || albumPhotoIds.isEmpty()) return;

        long validCount = photoRepository.countByIdsAndWorkspaceId(albumPhotoIds, workspaceId);
        if (validCount != albumPhotoIds.size()) {
            throw new SoflyException(ErrorCode.PHOTO_NOT_FOUND);
        }

        Map<Long, Photo> photoMap = photoRepository.findAllByIdsAndWorkspaceId(albumPhotoIds, workspaceId)
                .stream()
                .collect(Collectors.toMap(Photo::getId, p -> p));

        List<String> copiedKeys = new ArrayList<>();
        try {
            for (int i = 0; i < albumPhotoIds.size(); i++) {
                Photo photo = photoMap.get(albumPhotoIds.get(i));
                if (photo == null) continue;
                String sourceKey = photo.getS3Key();
                String ext = sourceKey.contains(".") ? sourceKey.substring(sourceKey.lastIndexOf('.') + 1) : "jpg";
                String destKey = String.format("sns-posts/%d/%s.%s", workspaceId, UUID.randomUUID(), ext);
                s3Service.copyObject(sourceKey, destKey);
                copiedKeys.add(destKey);
                post.addImage(SnsPostImage.of(post, destKey, s3Service.buildObjectUrl(destKey), startIndex + i));
            }
        } catch (RuntimeException e) {
            for (String key : copiedKeys) {
                try { s3Service.deleteObject(key); } catch (Exception ex) {
                    log.warn("S3 rollback 실패: {}", key);
                }
            }
            throw e;
        }
    }

    /**
     * 워크스페이스 공개범위 기준 접근 제어
     * PUBLIC          → 누구나
     * FOLLOWERS_ONLY  → 워크스페이스 멤버 + 소유자 팔로워
     * PRIVATE         → 워크스페이스 멤버만
     */
    private void checkWorkspaceVisibilityAccess(Long workspaceId, Long userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getVisibility() == WorkspaceVisibility.PUBLIC) return;
        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) return;
        if (workspace.getVisibility() == WorkspaceVisibility.FOLLOWERS_ONLY
                && userFollowRepository.existsByFollowerIdAndFollowingId(userId, workspace.getOwner().getId())) return;
        throw new SnsException(SnsErrorCode.SNS_POST_FORBIDDEN);
    }

    private void validateMember(Long workspaceId, Long userId) {
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_ACCESS_DENIED));
    }

    private boolean isWorkspaceOwner(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(m -> m.getRole() == com.sofly.core.domain.workspace.entity.WorkspaceMember.MemberRole.OWNER)
                .orElse(false);
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

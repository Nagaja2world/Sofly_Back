package com.sofly.core.domain.album.service;

import com.sofly.core.domain.album.dto.AlbumResponse;
import com.sofly.core.domain.album.dto.DownloadUrlResponse;
import com.sofly.core.domain.album.dto.PhotoResponse;
import com.sofly.core.domain.album.entity.Album;
import com.sofly.core.domain.album.entity.Photo;
import com.sofly.core.domain.album.repository.AlbumRepository;
import com.sofly.core.domain.album.repository.PhotoRepository;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.workspace.entity.WorkspaceMember;
import com.sofly.core.domain.workspace.entity.WorkspaceMember.MemberRole;
import com.sofly.core.domain.workspace.repository.WorkspaceMemberRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB
    private static final int MAX_FILE_COUNT = 20;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic"
    );

    private final AlbumRepository albumRepository;
    private final PhotoRepository photoRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    /** 앨범 + 사진 목록 조회 (앨범 없으면 빈 목록 반환) */
    public AlbumResponse getAlbum(Long workspaceId, Long userId) {
        validateMember(workspaceId, userId);
        return albumRepository.findByWorkspaceId(workspaceId)
                .map(album -> {
                    List<PhotoResponse> photos = photoRepository
                            .findByAlbumIdWithUploaderOrderByCreatedAtDesc(album.getId())
                            .stream()
                            .map(PhotoResponse::from)
                            .toList();
                    return new AlbumResponse(album.getId(), workspaceId, photos);
                })
                .orElse(new AlbumResponse(null, workspaceId, List.of()));
    }

    /** 사진 업로드 (단건 또는 다건) */
    @Transactional
    public List<PhotoResponse> uploadPhotos(Long workspaceId, Long userId, List<MultipartFile> files) {
        validateFiles(files);
        validateUploadPermission(workspaceId, userId);
        Album album = getOrCreateAlbum(workspaceId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SoflyException(ErrorCode.USER_NOT_FOUND));

        List<Photo> photos = new ArrayList<>();
        List<String> uploadedKeys = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                String s3Key = String.format("albums/%d/%s.%s", workspaceId, UUID.randomUUID(), resolveExtension(file));
                s3Service.uploadFile(file, s3Key);
                uploadedKeys.add(s3Key);
                photos.add(Photo.of(album, user, s3Key, s3Service.buildObjectUrl(s3Key), null, null, null));
            }

            photoRepository.saveAll(photos);
            return photos.stream().map(PhotoResponse::from).toList();
        } catch (RuntimeException e) {
            for (String key : uploadedKeys) {
                try {
                    s3Service.deleteObject(key);
                } catch (RuntimeException ex) {
                    // 정리 작업 중 오류는 무시하여 원래 예외가 유실되지 않도록 함
                }
            }
            throw e;
        }
    }

    /** 사진 삭제 (S3 + DB) */
    @Transactional
    public void deletePhoto(Long workspaceId, Long userId, Long photoId) {
        WorkspaceMember member = validateMember(workspaceId, userId);
        Photo photo = photoRepository.findByIdWithDetails(photoId)
                .orElseThrow(() -> new SoflyException(ErrorCode.PHOTO_NOT_FOUND));

        if (!photo.getAlbum().getWorkspace().getId().equals(workspaceId)) {
            throw new SoflyException(ErrorCode.PHOTO_NOT_FOUND);
        }

        boolean isOwnerOrEditor = member.getRole() == MemberRole.OWNER || member.getRole() == MemberRole.EDITOR;
        boolean isUploader = photo.getUploadedBy().getId().equals(userId);

        if (!isOwnerOrEditor && !isUploader) {
            throw new SoflyException(ErrorCode.DELETE_PERMISSION_DENIED);
        }

        s3Service.deleteObject(photo.getS3Key());
        photoRepository.delete(photo);
    }

    /** 다운로드 Presigned URL 발급 */
    public DownloadUrlResponse generateDownloadUrl(Long workspaceId, Long userId, Long photoId) {
        validateMember(workspaceId, userId);
        Photo photo = photoRepository.findByIdWithDetails(photoId)
                .orElseThrow(() -> new SoflyException(ErrorCode.PHOTO_NOT_FOUND));

        if (!photo.getAlbum().getWorkspace().getId().equals(workspaceId)) {
            throw new SoflyException(ErrorCode.PHOTO_NOT_FOUND);
        }

        return new DownloadUrlResponse(s3Service.generatePresignedDownloadUrl(photo.getS3Key()));
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────

    private WorkspaceMember validateMember(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_ACCESS_DENIED));
    }

    private void validateUploadPermission(Long workspaceId, Long userId) {
        WorkspaceMember member = validateMember(workspaceId, userId);
        if (member.getRole() == MemberRole.VIEWER) {
            throw new SoflyException(ErrorCode.UPLOAD_PERMISSION_DENIED);
        }
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files.isEmpty()) {
            throw new SoflyException(ErrorCode.INVALID_INPUT);
        }
        if (files.size() > MAX_FILE_COUNT) {
            throw new SoflyException(ErrorCode.TOO_MANY_FILES);
        }
        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new SoflyException(ErrorCode.FILE_TOO_LARGE);
            }
            if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
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

    private Album getOrCreateAlbum(Long workspaceId) {
        return albumRepository.findByWorkspaceId(workspaceId)
                .orElseGet(() -> {
                    var workspace = workspaceRepository.findById(workspaceId)
                            .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_NOT_FOUND));
                    return albumRepository.save(Album.of(workspace));
                });
    }
}

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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final PhotoRepository photoRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    /** 앨범 + 사진 목록 조회 */
    @Transactional
    public AlbumResponse getAlbum(Long workspaceId, Long userId) {
        validateMember(workspaceId, userId);
        Album album = getOrCreateAlbum(workspaceId);
        List<PhotoResponse> photos = photoRepository.findByAlbumIdOrderByCreatedAtDesc(album.getId())
                .stream()
                .map(PhotoResponse::from)
                .toList();
        return new AlbumResponse(album.getId(), workspaceId, photos);
    }

    /** 사진 업로드 (단건 또는 다건) */
    @Transactional
    public List<PhotoResponse> uploadPhotos(Long workspaceId, Long userId, List<MultipartFile> files) {
        validateUploadPermission(workspaceId, userId);
        Album album = getOrCreateAlbum(workspaceId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SoflyException(ErrorCode.USER_NOT_FOUND));

        List<Photo> photos = files.stream().map(file -> {
            String ext = extractExtension(file.getOriginalFilename());
            String s3Key = String.format("albums/%d/%s.%s", workspaceId, UUID.randomUUID(), ext);
            String url = s3Service.uploadFile(file, s3Key);
            return Photo.of(album, user, s3Key, url, null, null, null);
        }).toList();

        photoRepository.saveAll(photos);
        return photos.stream().map(PhotoResponse::from).toList();
    }

    /** 사진 삭제 (S3 + DB) */
    @Transactional
    public void deletePhoto(Long workspaceId, Long userId, Long photoId) {
        WorkspaceMember member = validateMember(workspaceId, userId);
        Photo photo = photoRepository.findById(photoId)
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
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new SoflyException(ErrorCode.PHOTO_NOT_FOUND));

        if (!photo.getAlbum().getWorkspace().getId().equals(workspaceId)) {
            throw new SoflyException(ErrorCode.PHOTO_NOT_FOUND);
        }

        String downloadUrl = s3Service.generatePresignedDownloadUrl(photo.getS3Key());
        return new DownloadUrlResponse(downloadUrl);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────

    private WorkspaceMember validateMember(Long workspaceId, Long userId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new SoflyException(ErrorCode.WORKSPACE_NOT_FOUND);
        }
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_ACCESS_DENIED));
    }

    private void validateUploadPermission(Long workspaceId, Long userId) {
        WorkspaceMember member = validateMember(workspaceId, userId);
        if (member.getRole() == MemberRole.VIEWER) {
            throw new SoflyException(ErrorCode.UPLOAD_PERMISSION_DENIED);
        }
    }

    private Album getOrCreateAlbum(Long workspaceId) {
        return albumRepository.findByWorkspaceId(workspaceId)
                .orElseGet(() -> {
                    var workspace = workspaceRepository.findById(workspaceId)
                            .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_NOT_FOUND));
                    return albumRepository.save(Album.of(workspace));
                });
    }

    private String extractExtension(String fileName) {
        if (fileName == null) return "jpg";
        int idx = fileName.lastIndexOf('.');
        return idx >= 0 ? fileName.substring(idx + 1).toLowerCase() : "jpg";
    }
}

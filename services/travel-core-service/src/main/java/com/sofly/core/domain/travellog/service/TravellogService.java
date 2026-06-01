package com.sofly.core.domain.travellog.service;

import com.sofly.core.domain.album.dto.PhotoResponse;
import com.sofly.core.domain.album.entity.Photo;
import com.sofly.core.domain.album.repository.PhotoRepository;
import com.sofly.core.domain.album.service.AlbumService;
import com.sofly.core.domain.travellog.dto.TravellogCreateRequest;
import com.sofly.core.domain.travellog.dto.TravellogResponse;
import com.sofly.core.domain.travellog.dto.TravellogSummaryResponse;
import com.sofly.core.domain.travellog.dto.TravellogUpdateRequest;
import com.sofly.core.domain.travellog.entity.TravelLog;
import com.sofly.core.domain.travellog.repository.TravellogRepository;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.sns.repository.UserFollowRepository;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.entity.WorkspaceMember;
import com.sofly.core.domain.workspace.entity.WorkspaceVisibility;
import com.sofly.core.domain.workspace.repository.WorkspaceMemberRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import com.sofly.core.global.security.util.SecurityUtils;
import com.sofly.core.global.security.workspace.RequireWorkspaceMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravellogService {

    private final TravellogRepository travellogRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final AlbumService albumService;
    private final PhotoRepository photoRepository;

    // ── 조회 ─────────────────────────────────────────────────

    @RequireWorkspaceMember
    public TravellogResponse getTravelLog(Long workspaceId, Long logId) {
        TravelLog travelLog = travellogRepository.findByIdWithPhotos(logId)
                .orElseThrow(() -> new SoflyException(ErrorCode.TRAVEL_LOG_NOT_FOUND));

        if (!travelLog.getWorkspace().getId().equals(workspaceId)) {
            throw new SoflyException(ErrorCode.TRAVEL_LOG_NOT_FOUND);
        }

        return TravellogResponse.from(travelLog);
    }

    public List<TravellogSummaryResponse> getTravelLogs(Long workspaceId) {
        requireReadAccess(workspaceId);
        return travellogRepository.findAllSummaryByWorkspaceId(workspaceId);
    }

    public List<TravellogResponse> getTravelLogsWithDetails(Long workspaceId) {
        requireReadAccess(workspaceId);
        return travellogRepository.findAllWithDetailsByWorkspaceId(workspaceId)
                .stream()
                .sorted(Comparator.comparing(TravelLog::getCreatedAt))
                .map(TravellogResponse::from)
                .toList();
    }

    // ── 생성 ─────────────────────────────────────────────────

    @Transactional
    @RequireWorkspaceMember(minRole = WorkspaceMember.MemberRole.EDITOR)
    public TravellogResponse createTravelLog(Long workspaceId, Long userId, TravellogCreateRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_NOT_FOUND));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new SoflyException(ErrorCode.USER_NOT_FOUND));

        TravelLog travelLog = TravelLog.builder()
                .mainTitle(request.mainTitle())
                .travelDate(request.travelDate())
                .title(request.title())
                .content(request.content())
                .weather(request.weather())
                .workspace(workspace)
                .author(author)
                .build();

        workspace.touch();
        return TravellogResponse.from(travellogRepository.save(travelLog));
    }

    // ── 수정 ─────────────────────────────────────────────────

    @Transactional
    @RequireWorkspaceMember(minRole = WorkspaceMember.MemberRole.EDITOR)
    public TravellogResponse updateTravelLog(Long workspaceId, Long logId, TravellogUpdateRequest request) {
        TravelLog travelLog = travellogRepository.findByIdWithPhotos(logId)
                .orElseThrow(() -> new SoflyException(ErrorCode.TRAVEL_LOG_NOT_FOUND));

        if (!travelLog.getWorkspace().getId().equals(workspaceId)) {
            throw new SoflyException(ErrorCode.TRAVEL_LOG_NOT_FOUND);
        }

        travelLog.getWorkspace().touch();
        travelLog.update(request);
        return TravellogResponse.from(travelLog);
    }

    // ── 삭제 ─────────────────────────────────────────────────

    @Transactional
    @RequireWorkspaceMember(minRole = WorkspaceMember.MemberRole.EDITOR)
    public void deleteTravelLog(Long workspaceId, Long logId) {
        TravelLog travelLog = travellogRepository.findById(logId)
                .orElseThrow(() -> new SoflyException(ErrorCode.TRAVEL_LOG_NOT_FOUND));

        if (!travelLog.getWorkspace().getId().equals(workspaceId)) {
            throw new SoflyException(ErrorCode.TRAVEL_LOG_NOT_FOUND);
        }

        travelLog.getWorkspace().touch();
        travellogRepository.delete(travelLog);
    }

    // ── 사진 연결 ─────────────────────────────────────────────

    @Transactional
    @RequireWorkspaceMember(minRole = WorkspaceMember.MemberRole.EDITOR)
    public TravellogResponse uploadAndAttachPhotos(Long workspaceId, Long userId, Long logId, List<MultipartFile> files) {
        List<PhotoResponse> photoResponses = albumService.uploadPhotos(workspaceId, userId, files);

        List<Long> photoIds = photoResponses.stream()
                .map(PhotoResponse::id)
                .toList();

        return addPhotosToTravelLog(logId, photoIds);
    }

    @Transactional
    @RequireWorkspaceMember(minRole = WorkspaceMember.MemberRole.EDITOR)
    public TravellogResponse attachAlbumPhotos(Long workspaceId, Long logId, List<Long> photoIds) {
        long validCount = photoRepository.countByIdsAndWorkspaceId(photoIds, workspaceId);
        if (validCount != photoIds.size()) {
            throw new SoflyException(ErrorCode.PHOTO_NOT_FOUND);
        }
        return addPhotosToTravelLog(logId, photoIds);
    }

    @Transactional
    @RequireWorkspaceMember(minRole = WorkspaceMember.MemberRole.EDITOR)
    public TravellogResponse removePhotos(Long workspaceId, Long logId, List<Long> photoIds) {
        TravelLog travelLog = travellogRepository.findByIdWithPhotos(logId)
                .orElseThrow(() -> new SoflyException(ErrorCode.TRAVEL_LOG_NOT_FOUND));

        Set<Long> attachedPhotoIds = travelLog.getPhotos().stream()
                .map(Photo::getId)
                .collect(Collectors.toSet());

        for (Long requestedId : photoIds) {
            if (!attachedPhotoIds.contains(requestedId)) {
                throw new SoflyException(ErrorCode.PHOTO_NOT_FOUND);
            }
        }

        List<Photo> photos = photoRepository.findAllById(photoIds);
        photos.forEach(travelLog::removePhoto);
        return TravellogResponse.from(travelLog);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────

    /** PUBLIC: 누구나 / FOLLOWERS_ONLY: 팔로워+멤버 / PRIVATE: 멤버만 */
    private void requireReadAccess(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getVisibility() == WorkspaceVisibility.PUBLIC) return;
        Long userId = SecurityUtils.getCurrentUserId();
        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) return;
        if (workspace.getVisibility() == WorkspaceVisibility.FOLLOWERS_ONLY
                && userFollowRepository.existsByFollowerIdAndFollowingId(userId, workspace.getOwner().getId())) return;
        throw new SoflyException(ErrorCode.WORKSPACE_ACCESS_DENIED);
    }

    private TravellogResponse addPhotosToTravelLog(Long logId, List<Long> photoIds) {
        TravelLog travelLog = travellogRepository.findByIdWithPhotos(logId)
                .orElseThrow(() -> new SoflyException(ErrorCode.TRAVEL_LOG_NOT_FOUND));

        Set<Long> existingPhotoIds = travelLog.getPhotos().stream()
                .map(Photo::getId)
                .collect(Collectors.toSet());

        List<Photo> photos = photoRepository.findAllById(photoIds);
        photos.stream()
                .filter(p -> !existingPhotoIds.contains(p.getId()))
                .forEach(travelLog::addPhoto);

        travelLog.getWorkspace().touch();
        return TravellogResponse.from(travelLog);
    }
}

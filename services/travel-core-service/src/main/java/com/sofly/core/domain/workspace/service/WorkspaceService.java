package com.sofly.core.domain.workspace.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.sofly.core.domain.album.service.S3Service;
import com.sofly.core.domain.conquest.service.AirportInfoService;
import com.sofly.core.domain.workspace.dto.request.*;
import com.sofly.core.domain.workspace.dto.response.*;
import com.sofly.core.domain.workspace.entity.*;
import com.sofly.core.domain.workspace.entity.WorkspaceInvitation.InvitationStatus;
import com.sofly.core.domain.workspace.repository.SavedPlaceRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceInvitationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sofly.core.domain.user.code.UserErrorCode;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.exception.UserException;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.sns.repository.UserFollowRepository;
import com.sofly.core.domain.workspace.code.WorkspaceErrorCode;
import com.sofly.core.domain.workspace.entity.WorkspaceMember.MemberRole;
import com.sofly.core.domain.workspace.exception.WorkspaceException;
import com.sofly.core.domain.workspace.repository.SavedFlightRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceMemberRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import com.sofly.core.domain.messaging.entity.MessagingRoomMember;
import com.sofly.core.domain.messaging.enums.ChatRoomType;
import com.sofly.core.domain.messaging.repository.MessagingRoomMemberRepository;
import com.sofly.core.domain.messaging.repository.MessagingRoomRepository;
import com.sofly.core.global.kafka.dto.FlightSavedMessage;
import com.sofly.core.global.kafka.dto.InvitationCreatedMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private static final String INVITE_REDIS_KEY_PREFIX = "invite:";
    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserFollowRepository userFollowRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final SavedFlightRepository savedFlightRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final UserRepository userRepository;
    private final AirportInfoService airportInfoService;
    private final MessagingRoomRepository messagingRoomRepository;
    private final MessagingRoomMemberRepository messagingRoomMemberRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final S3Service s3Service;

    @Value("${sofly.oauth2.redirect-uri}")
    private String baseUrl;

    // ── 워크스페이스 생성 ──────────────────────────────────────

    @Transactional
    public WorkspaceResponse createWorkspace(Long userId, CreateWorkspaceRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        Workspace workspace = Workspace.builder()
                .title(request.getTitle())
                .destination(request.getDestination())
                .countryCode(normalizeCountryCode(request.getCountryCode()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .headcount(request.getHeadcount())
                .coverImageUrl(request.getCoverImageUrl())
                .owner(owner)
                .build();

        // 소유자를 멤버 목록에도 OWNER로 등록
        WorkspaceMember ownerMember = WorkspaceMember.ofOwner(workspace, owner);
        workspace.addMember(ownerMember);

        workspaceRepository.save(workspace);
        return WorkspaceResponse.from(workspace);
    }

    // ── 내 워크스페이스 목록 조회 ──────────────────────────────

    public List<WorkspaceResponse> getMyWorkspaces(Long userId) {
        return workspaceRepository.findAllWithOwnerByUserId(userId).stream()
                .map(WorkspaceResponse::from)
                .toList();
    }

    // ── 워크스페이스 상세 조회 ─────────────────────────────────

    public WorkspaceResponse getWorkspace(Long userId, Long workspaceId) {
        Workspace workspace = findWorkspaceById(workspaceId);
        validateMember(workspaceId, userId);
        return WorkspaceResponse.from(workspace);
    }

    // ── 워크스페이스 수정 ──────────────────────────────────────

    @Transactional
    public WorkspaceResponse updateWorkspace(Long userId, Long workspaceId, UpdateWorkspaceRequest request) {
        Workspace workspace = findWorkspaceById(workspaceId);
        validateOwner(workspace, userId);

        workspace.update(
                request.getTitle(),
                request.getDestination(),
                request.getCountryCode(),
                request.getStartDate(),
                request.getEndDate(),
                request.getHeadcount(),
                request.getCoverImageUrl()
        );

        return WorkspaceResponse.from(workspace);
    }

    // ── 커버 이미지 업로드 ─────────────────────────────────────

    @Transactional
    public WorkspaceResponse uploadCoverImage(Long userId, Long workspaceId, MultipartFile file) {
        Workspace workspace = findWorkspaceById(workspaceId);
        validateOwner(workspace, userId);

        String ext = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String s3Key = "workspaces/" + workspaceId + "/cover/" + java.util.UUID.randomUUID() + ext;

        String imageUrl = s3Service.uploadFile(file, s3Key, true);
        workspace.updateCoverImage(imageUrl);

        return WorkspaceResponse.from(workspace);
    }

    // ── 워크스페이스 삭제 ──────────────────────────────────────

    @Transactional
    public void deleteWorkspace(Long userId, Long workspaceId) {
        Workspace workspace = findWorkspaceById(workspaceId);
        validateOwner(workspace, userId);
        workspaceRepository.delete(workspace);
    }

    // ── 초대 코드 생성 ─────────────────────────────────────────

    @Transactional
    public InviteCodeResponse generateInviteCode(Long userId, Long workspaceId) {
        Workspace workspace = findWorkspaceById(workspaceId);
        validateOwner(workspace, userId);

        String code = UUID.randomUUID().toString();
        workspace.generateInviteCode(code);

        return InviteCodeResponse.of(code, baseUrl);
    }

    // ── 초대 요청 생성 ─────────────────────────────────────────

    @Transactional
    public InvitationResponse inviteMember(Long requesterId, Long workspaceId, Long targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw new WorkspaceException(WorkspaceErrorCode.CANNOT_INVITE_SELF);
        }

        Workspace workspace = findWorkspaceById(workspaceId);
        validateOwner(workspace, requesterId);

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUserId)) {
            throw new WorkspaceException(WorkspaceErrorCode.ALREADY_WORKSPACE_MEMBER);
        }
        if (workspaceInvitationRepository.existsByWorkspaceIdAndInviteeIdAndStatus(
                workspaceId, targetUserId, InvitationStatus.PENDING)) {
            throw new WorkspaceException(WorkspaceErrorCode.ALREADY_PENDING_INVITATION);
        }

        User inviter = userRepository.findById(requesterId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        User invitee = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .inviter(inviter)
                .invitee(invitee)
                .expiresAt(LocalDateTime.now().plus(INVITE_TTL))
                .build();

        workspaceInvitationRepository.save(invitation);

        // Redis에 TTL과 함께 토큰 등록
        stringRedisTemplate.opsForValue().set(
                INVITE_REDIS_KEY_PREFIX + invitation.getId(), "1", INVITE_TTL);

        // Kafka로 알림 이벤트 발행
        kafkaTemplate.send("workspace.invitation", new InvitationCreatedMessage(
                invitation.getId(), workspace.getId(), workspace.getTitle(),
                inviter.getNickname(), targetUserId));

        return InvitationResponse.from(invitation);
    }

    // ── 초대 수락 ──────────────────────────────────────────────

    @Transactional
    public WorkspaceMemberResponse acceptInvitation(Long userId, Long invitationId) {
        WorkspaceInvitation invitation = workspaceInvitationRepository
                .findByIdAndInviteeId(invitationId, userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.INVITATION_NOT_FOUND));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new WorkspaceException(WorkspaceErrorCode.INVITATION_ALREADY_PROCESSED);
        }

        String redisKey = INVITE_REDIS_KEY_PREFIX + invitationId;
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(redisKey))) {
            throw new WorkspaceException(WorkspaceErrorCode.INVITATION_EXPIRED);
        }

        Workspace workspace = invitation.getWorkspace();

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), userId)) {
            throw new WorkspaceException(WorkspaceErrorCode.ALREADY_WORKSPACE_MEMBER);
        }

        invitation.accept();
        stringRedisTemplate.delete(redisKey);

        User invitee = invitation.getInvitee();
        WorkspaceMember newMember = WorkspaceMember.ofViewer(workspace, invitee);
        workspace.addMember(newMember);

        addToWorkspaceMessagingRoom(workspace.getId(), invitee.getId());

        return WorkspaceMemberResponse.from(newMember);
    }

    // ── 초대 거절 ──────────────────────────────────────────────

    @Transactional
    public void rejectInvitation(Long userId, Long invitationId) {
        WorkspaceInvitation invitation = workspaceInvitationRepository
                .findByIdAndInviteeId(invitationId, userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.INVITATION_NOT_FOUND));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new WorkspaceException(WorkspaceErrorCode.INVITATION_ALREADY_PROCESSED);
        }

        invitation.reject();
        stringRedisTemplate.delete(INVITE_REDIS_KEY_PREFIX + invitationId);
    }

    // ── 내 초대 목록 조회 ──────────────────────────────────────

    public List<InvitationResponse> getMyInvitations(Long userId) {
        return workspaceInvitationRepository
                .findAllByInviteeIdAndStatus(userId, InvitationStatus.PENDING).stream()
                .filter(i -> !i.isExpired())
                .map(InvitationResponse::from)
                .toList();
    }

    // ── 초대 코드로 멤버 가입 ──────────────────────────────────

    @Transactional
    public WorkspaceResponse joinWorkspace(Long userId, String inviteCode) {
        Workspace workspace = workspaceRepository.findWithOwnerByInviteCode(inviteCode)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.INVALID_INVITE_CODE));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), userId)) {
            throw new WorkspaceException(WorkspaceErrorCode.ALREADY_WORKSPACE_MEMBER);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        WorkspaceMember newMember = WorkspaceMember.ofViewer(workspace, user);
        workspace.addMember(newMember);

        addToWorkspaceMessagingRoom(workspace.getId(), userId);

        return WorkspaceResponse.from(workspace);
    }

    // ── 멤버 목록 조회 ─────────────────────────────────────────

    public List<WorkspaceMemberResponse> getMembers(Long userId, Long workspaceId) {
        findWorkspaceById(workspaceId);
        validateMember(workspaceId, userId);
        return workspaceMemberRepository.findAllWithUserByWorkspaceId(workspaceId).stream()
                .map(WorkspaceMemberResponse::from)
                .toList();
    }

    // ── 멤버 역할 변경 ─────────────────────────────────────────

    @Transactional
    public WorkspaceMemberResponse updateMemberRole(Long userId, Long workspaceId,
                                                    Long memberId, UpdateMemberRoleRequest request) {
        Workspace workspace = findWorkspaceById(workspaceId);
        validateOwner(workspace, userId);

        if (request.getRole() == MemberRole.OWNER) {
            throw new WorkspaceException(WorkspaceErrorCode.CANNOT_ASSIGN_OWNER_ROLE);
        }

        WorkspaceMember member = workspaceMemberRepository.findById(memberId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        member.updateRole(request.getRole());
        return WorkspaceMemberResponse.from(member);
    }

    // ── 멤버 내보내기 / 탈퇴 ──────────────────────────────────

    @Transactional
    public void removeMember(Long userId, Long workspaceId, Long memberId) {
        Workspace workspace = findWorkspaceById(workspaceId);

        WorkspaceMember target = workspaceMemberRepository.findById(memberId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        boolean isSelf = target.getUser().getId().equals(userId);
        boolean isOwner = workspace.isOwner(userId);

        if (isSelf && isOwner) {
            throw new WorkspaceException(WorkspaceErrorCode.OWNER_CANNOT_LEAVE);
        }
        if (!isSelf && !isOwner) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_FORBIDDEN);
        }

        workspaceMemberRepository.delete(target);
    }

    // ── 항공편 저장 ────────────────────────────────────────────

    @Transactional
    public SavedFlightResponse saveFlight(Long userId, Long workspaceId, SaveFlightRequest request) {
        Workspace workspace = findWorkspaceById(workspaceId);
        validateMember(workspaceId, userId);

        SavedFlight flight = SavedFlight.builder()
                .workspace(workspace)
                .flightNumber(request.getFlightNumber())
                .airline(request.getAirline())
                .airlineLogo(request.getAirlineLogo())
                .planeType(request.getPlaneType())
                .cabinClass(request.getCabinClass())
                .departureAirport(request.getDepartureAirport())
                .departureCity(request.getDepartureCity())
                .departureCountry(request.getDepartureCountry())
                .departureTerminal(request.getDepartureTerminal())
                .arrivalAirport(request.getArrivalAirport())
                .arrivalCity(request.getArrivalCity())
                .arrivalCountry(request.getArrivalCountry())
                .arrivalTerminal(request.getArrivalTerminal())
                .departureTime(request.getDepartureTime().toLocalDateTime())
                .arrivalTime(request.getArrivalTime().toLocalDateTime())
                .durationMinutes(request.getDurationMinutes())
                .totalPrice(request.getTotalPrice())
                .baseFare(request.getBaseFare())
                .tax(request.getTax())
                .platformFee(request.getPlatformFee())
                .currencyCode(request.getCurrencyCode())
                .checkedBaggageKg(request.getCheckedBaggageKg())
                .checkedBaggagePiece(request.getCheckedBaggagePiece())
                .cabinBaggageKg(request.getCabinBaggageKg())
                .personalItemIncluded(request.getPersonalItemIncluded())
                .bookingToken(request.getBookingToken())
                .offerReference(request.getOfferReference())
                .deepLinkUrl(request.getDeepLinkUrl())
                .flightType(request.getFlightType())
                .build();

        savedFlightRepository.save(flight);
        workspace.touch();

        // 도착 공항의 국가코드로 워크스페이스 countryCode 자동 업데이트
        airportInfoService.findByIata(request.getArrivalAirport())
                .ifPresent(info -> workspace.updateCountryCode(info.countryCode()));

        // 정복 지도: 도착 국가/도시를 PLANNED 상태로 자동 반영
        List<Long> memberIds = workspace.getMembers().stream()
                .map(member -> member.getUser().getId())
                .toList();
        kafkaTemplate.send("flight.saved", new FlightSavedMessage(
                workspace.getId(), memberIds,
                request.getDepartureAirport(), request.getArrivalAirport(), request.getDepartureTime()
        ));

        return SavedFlightResponse.from(flight);
    }

    // ── 항공편 목록 조회 ───────────────────────────────────────

    public List<SavedFlightResponse> getFlights(Long userId, Long workspaceId) {
        Workspace workspace = findWorkspaceById(workspaceId);
        requireReadAccess(workspace, userId);
        return savedFlightRepository.findAllByWorkspaceId(workspaceId).stream()
                .map(SavedFlightResponse::from)
                .toList();
    }

    // ── 항공편 수정 ────────────────────────────────────────────

    @Transactional
    public SavedFlightResponse updateFlight(Long userId, Long workspaceId, Long flightId, UpdateFlightRequest request) {
        Workspace workspace = findWorkspaceById(workspaceId);
        validateMember(workspaceId, userId);

        SavedFlight flight = savedFlightRepository.findByIdAndWorkspaceId(flightId, workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.SAVED_FLIGHT_NOT_FOUND));

        flight.update(
                request.getFlightNumber(), request.getAirline(), request.getAirlineLogo(),
                request.getPlaneType(), request.getCabinClass(),
                request.getDepartureAirport(), request.getDepartureCity(), request.getDepartureCountry(), request.getDepartureTerminal(),
                request.getArrivalAirport(), request.getArrivalCity(), request.getArrivalCountry(), request.getArrivalTerminal(),
                request.getDepartureTime() != null ? request.getDepartureTime().toLocalDateTime() : null,
                request.getArrivalTime() != null ? request.getArrivalTime().toLocalDateTime() : null,
                request.getDurationMinutes(),
                request.getTotalPrice(), request.getBaseFare(), request.getTax(), request.getPlatformFee(), request.getCurrencyCode(),
                request.getCheckedBaggageKg(), request.getCheckedBaggagePiece(), request.getCabinBaggageKg(),
                request.getPersonalItemIncluded(), request.getBookingToken(), request.getOfferReference(),
                request.getDeepLinkUrl(), request.getFlightType()
        );

        workspace.touch();
        return SavedFlightResponse.from(flight);
    }

    // ── 항공편 삭제 ────────────────────────────────────────────

    @Transactional
    public void deleteFlight(Long userId, Long workspaceId, Long flightId) {
        Workspace workspace = findWorkspaceById(workspaceId);
        validateMember(workspaceId, userId);

        SavedFlight flight = savedFlightRepository.findByIdAndWorkspaceId(flightId, workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.SAVED_FLIGHT_NOT_FOUND));

        savedFlightRepository.delete(flight);
        workspace.touch();
    }

    // ── savedPlace ───────────────────────────────────────
    @Transactional
    public SavedPlaceResponse savePlace(Long userId, Long workspaceId, SavePlaceRequest request){
        Workspace workspace = findWorkspaceById(workspaceId);
        validateMember(workspaceId, userId);

        if(savedPlaceRepository.existsByWorkspaceIdAndPlaceId(workspaceId, request.placeId())){
            throw new WorkspaceException(WorkspaceErrorCode.ALREADY_SAVED_PLACE);
        }

        SavedPlace savedPlace = SavedPlace.builder()
                .workspace(workspace)
                .placeId(request.placeId())
                .name(request.name())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .primaryType(request.primaryType())
                .photoReference(request.photoReference())
                .rating(request.rating())
                .googleMapsUri(request.googleMapsUri())
                .build();

        savedPlaceRepository.save(savedPlace);

        return SavedPlaceResponse.from(savedPlace);
    }

    public List<SavedPlaceResponse> getSavedPlaces(Long userId, Long workspaceId){
        findWorkspaceById(workspaceId);
        validateMember(workspaceId, userId);

        return savedPlaceRepository.findAllByWorkspaceId(workspaceId).stream()
                .map(SavedPlaceResponse::from)
                .toList();
    }

    @Transactional
    public void deleteSavedPlace(Long userId, Long workspaceId, Long savedPlaceId){
        findWorkspaceById(workspaceId);
        validateMember(workspaceId, userId);

        SavedPlace savedPlace = savedPlaceRepository.findByIdAndWorkspaceId(savedPlaceId, workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.SAVED_PLACE_NOT_FOUND));

        savedPlaceRepository.delete(savedPlace);
    }

    // ── sns ─────────────────────────────────────────────
    @Transactional
    public void changeVisibility(Long userId, Long workspaceId, WorkspaceVisibility workspaceVisibility) {
        Workspace workspace = findWorkspaceById(workspaceId);
        validateOwner(workspace, userId);
        workspace.changeVisibility(workspaceVisibility);
    }


    // ── 내부 헬퍼 ─────────────────────────────────────────────

    private void addToWorkspaceMessagingRoom(Long workspaceId, Long userId) {
        messagingRoomRepository.findByTypeAndWorkspaceId(ChatRoomType.WORKSPACE, workspaceId)
                .ifPresent(room -> {
                    boolean alreadyMember = messagingRoomMemberRepository
                            .existsByMessagingRoomIdAndUserId(room.getId(), userId);
                    if (!alreadyMember) {
                        messagingRoomMemberRepository.save(
                                MessagingRoomMember.builder()
                                        .messagingRoom(room)
                                        .userId(userId)
                                        .build()
                        );
                    }
                });
    }

    private Workspace findWorkspaceById(Long workspaceId) {
        return workspaceRepository.findWithOwnerById(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
    }

    private void validateOwner(Workspace workspace, Long userId) {
        if (!workspace.isOwner(userId)) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_FORBIDDEN);
        }
    }

    /** PUBLIC: 누구나 / FOLLOWERS_ONLY: 팔로워+멤버 / PRIVATE: 멤버만 */
    private void requireReadAccess(Workspace workspace, Long userId) {
        if (workspace.getVisibility() == WorkspaceVisibility.PUBLIC) return;
        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), userId)) return;
        if (workspace.getVisibility() == WorkspaceVisibility.FOLLOWERS_ONLY
                && userFollowRepository.existsByFollowerIdAndFollowingId(userId, workspace.getOwner().getId())) return;
        throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_FORBIDDEN);
    }

    private void validateMember(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_FORBIDDEN);
        }
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }
}

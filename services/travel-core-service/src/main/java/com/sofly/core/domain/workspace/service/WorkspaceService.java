package com.sofly.core.domain.workspace.service;

import java.util.List;
import java.util.UUID;

import com.sofly.core.domain.workspace.dto.request.*;
import com.sofly.core.domain.workspace.dto.response.*;
import com.sofly.core.domain.workspace.entity.*;
import com.sofly.core.domain.workspace.repository.SavedPlaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofly.core.domain.user.code.UserErrorCode;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.exception.UserException;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.workspace.code.WorkspaceErrorCode;
import com.sofly.core.domain.workspace.entity.WorkspaceMember.MemberRole;
import com.sofly.core.domain.workspace.exception.WorkspaceException;
import com.sofly.core.domain.workspace.repository.SavedFlightRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceMemberRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import com.sofly.core.global.kafka.dto.FlightSavedMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SavedFlightRepository savedFlightRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
                .countryCode(request.getCountryCode())
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
                request.getStartDate(),
                request.getEndDate(),
                request.getHeadcount(),
                request.getCoverImageUrl()
        );

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
                .departureAirport(request.getDepartureAirport())
                .arrivalAirport(request.getArrivalAirport())
                .departureTime(request.getDepartureTime().toLocalDateTime())
                .arrivalTime(request.getArrivalTime().toLocalDateTime())
                .duration(request.getDuration())
                .price(request.getPrice())
                .flightType(request.getFlightType())
                .build();

        savedFlightRepository.save(flight);

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
        findWorkspaceById(workspaceId);
        validateMember(workspaceId, userId);
        return savedFlightRepository.findAllByWorkspaceId(workspaceId).stream()
                .map(SavedFlightResponse::from)
                .toList();
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

    private Workspace findWorkspaceById(Long workspaceId) {
        return workspaceRepository.findWithOwnerById(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
    }

    private void validateOwner(Workspace workspace, Long userId) {
        if (!workspace.isOwner(userId)) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_FORBIDDEN);
        }
    }

    private void validateMember(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_FORBIDDEN);
        }
    }
}

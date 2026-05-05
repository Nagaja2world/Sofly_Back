package com.sofly.core.domain.schedule.service;

import com.sofly.core.domain.schedule.dto.*;
import com.sofly.core.domain.schedule.entity.Schedule;
import com.sofly.core.domain.schedule.entity.ScheduleItem;
import com.sofly.core.domain.schedule.repository.ScheduleItemRepository;
import com.sofly.core.domain.schedule.repository.ScheduleRepository;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleItemRepository scheduleItemRepository;
    private final WorkspaceRepository workspaceRepository;

    // ── 조회 ────────────────────────────────────────────────

    // 워크스페이스 일정 버전 목록
    public List<ScheduleSummaryResponse> getSchedulesByWorkspace(Long workspaceId) {
        return scheduleRepository.findSummariesByWorkspaceId(workspaceId);
    }

    // 일정 단건 상세 (아이템 포함)
    public ScheduleResponse getSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findByIdWithItems(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));
        return ScheduleResponse.from(schedule);
    }

    // 워크스페이스 최신 버전 일정
    public ScheduleResponse getLatestSchedule(Long workspaceId) {
        Schedule schedule = scheduleRepository.findTopByWorkspaceIdOrderByVersionDesc(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("No schedule found for workspace: " + workspaceId));
        // items 패치 (findTop은 items 미포함)
        return getSchedule(schedule.getId());
    }

    // ── 생성 ────────────────────────────────────────────────

    // 새 일정 생성 (AI 결과 저장 또는 수동 생성)
    @Transactional
    public ScheduleResponse createSchedule(ScheduleCreateRequest request) {
        Workspace workspace = workspaceRepository.findById(request.workspaceId())
                .orElseThrow(() -> new EntityNotFoundException("Workspace not found: " + request.workspaceId()));

        int nextVersion = scheduleRepository.findMaxVersionByWorkspaceId(request.workspaceId()) + 1;

        Schedule schedule = Schedule.builder()
                .workspace(workspace)
                .title(request.title() != null ? request.title() : nextVersion + "차 일정")
                .version(nextVersion)
                .build();

        // 아이템 생성 후 연관관계 설정
        List<ScheduleItem> items = request.items().stream()
                .map(itemReq -> buildScheduleItem(itemReq, schedule))
                .toList();

        // Schedule 저장 (cascade로 items도 함께 저장)
        schedule.getItems().addAll(items);
        scheduleRepository.save(schedule);

        return ScheduleResponse.from(schedule);
    }

    // 기존 일정을 복사해서 새 버전 생성 (수정본 만들기)
    @Transactional
    public ScheduleResponse forkSchedule(Long scheduleId, String newTitle) {
        Schedule origin = scheduleRepository.findByIdWithItems(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));

        int nextVersion = scheduleRepository.findMaxVersionByWorkspaceId(
                origin.getWorkspace().getId()) + 1;

        Schedule forked = Schedule.builder()
                .workspace(origin.getWorkspace())
                .title(newTitle != null ? newTitle : nextVersion + "차 일정")
                .version(nextVersion)
                .build();

        List<ScheduleItem> copiedItems = origin.getItems().stream()
                .map(item -> ScheduleItem.builder()
                        .schedule(forked)
                        .day(item.getDay())
                        .orderIndex(item.getOrderIndex())
                        .visitTime(item.getVisitTime())
                        .category(item.getCategory())
                        .name(item.getName())
                        .address(item.getAddress())
                        .latitude(item.getLatitude())
                        .longitude(item.getLongitude())
                        .placeId(item.getPlaceId())
                        .photoReference(item.getPhotoReference())
                        .memo(item.getMemo())
                        .deepLinkUrl(item.getDeepLinkUrl())
                        .estimatedCost(item.getEstimatedCost())
                        .build())
                .toList();

        forked.getItems().addAll(copiedItems);
        scheduleRepository.save(forked);

        return ScheduleResponse.from(forked);
    }

    // ── 수정 ────────────────────────────────────────────────

    // 일정 제목 수정
    @Transactional
    public ScheduleResponse updateScheduleTitle(Long scheduleId, String title) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));
        schedule.updateTitle(title);
        return getSchedule(scheduleId);
    }

    // 아이템 단건 수정 (visitTime, memo, category)
    @Transactional
    public ScheduleItemResponse updateItem(Long scheduleId, Long itemId, ScheduleItemUpdateRequest request) {
        ScheduleItem item = scheduleItemRepository.findByScheduleIdAndId(scheduleId, itemId)
                .orElseThrow(() -> new EntityNotFoundException("ScheduleItem not found: " + itemId));
        item.update(
                request.visitTime(),
                request.memo(),
                request.category(),
                request.address(),
                request.estimatedCost(),
                request.name()
        );

        if (request.placeId() != null) item.setPlaceId(request.placeId());
        if (request.photoReference() != null) item.setPhotoReference(request.photoReference());
        if (request.latitude() != null) item.setLatitude(request.latitude());
        if (request.longitude() != null) item.setLongitude(request.longitude());

        return ScheduleItemResponse.from(item);
    }

    // 아이템 추가 (일정에 장소 하나 추가)
    @Transactional
    public ScheduleItemResponse addItem(Long scheduleId, ScheduleItemCreateRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));

        // 해당 일차의 맨 마지막 순서로 추가
        int nextOrder = scheduleItemRepository
                .findMaxOrderIndexByScheduleIdAndDay(scheduleId, request.day()) + 1;

        ScheduleItem item = ScheduleItem.builder()
                .schedule(schedule)
                .day(request.day())
                .orderIndex(nextOrder)
                .visitTime(request.visitTime())
                .category(request.category())
                .name(request.name())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .placeId(request.placeId())
                .photoReference(request.photoReference())
                .memo(request.memo())
                .deepLinkUrl(request.deepLinkUrl())
                .estimatedCost(request.estimatedCost())
                .build();

        scheduleItemRepository.save(item);
        return ScheduleItemResponse.from(item);
    }

    // D&D 단일 아이템 이동 (프론트는 itemId + targetDay + targetOrderIndex만 전송)
    @Transactional
    public void moveItem(Long scheduleId, Long itemId, ScheduleItemMoveRequest request) {
        ScheduleItem item = scheduleItemRepository.findByScheduleIdAndId(scheduleId, itemId)
                .orElseThrow(() -> new EntityNotFoundException("ScheduleItem not found: " + itemId));

        int oldDay = item.getDay();
        int oldIndex = item.getOrderIndex();
        int newDay = request.targetDay();
        int newIndex = request.targetOrderIndex();

        if (oldDay == newDay) {
            if (oldIndex == newIndex) return;
            if (oldIndex < newIndex) {
                // 앞→뒤 이동: 사이 아이템들을 앞으로 당김
                scheduleItemRepository.shiftOrderIndex(scheduleId, oldDay, oldIndex + 1, newIndex, -1);
            } else {
                // 뒤→앞 이동: 사이 아이템들을 뒤로 밀기
                scheduleItemRepository.shiftOrderIndex(scheduleId, oldDay, newIndex, oldIndex - 1, +1);
            }
        } else {
            // day 간 이동
            scheduleItemRepository.shiftOrderIndexFrom(scheduleId, oldDay, oldIndex + 1, -1);  // 원래 day 빈 자리 당기기
            scheduleItemRepository.shiftOrderIndexFrom(scheduleId, newDay, newIndex, +1);       // 새 day 자리 만들기
            item.moveToDay(newDay);
        }

        item.updateOrder(newIndex);
    }

    // ── 삭제 ────────────────────────────────────────────────

    // 아이템 단건 삭제
    @Transactional
    public void deleteItem(Long scheduleId, Long itemId) {
        ScheduleItem item = scheduleItemRepository.findByScheduleIdAndId(scheduleId, itemId)
                .orElseThrow(() -> new EntityNotFoundException("ScheduleItem not found: " + itemId));
        scheduleItemRepository.delete(item);
    }

    // 일정 전체 삭제
    @Transactional
    public void deleteSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));
        scheduleRepository.delete(schedule);  // orphanRemoval로 items도 cascade 삭제
    }

    // ── 딥링크 ──────────────────────────────────────────────

    @Transactional
    public void trackDeepLinkClick(Long scheduleId, Long itemId) {
        ScheduleItem item = scheduleItemRepository.findByScheduleIdAndId(scheduleId, itemId)
                .orElseThrow(() -> new EntityNotFoundException("ScheduleItem not found: " + itemId));
        item.incrementDeepLinkClick();
    }

    // ── 내부 헬퍼 ───────────────────────────────────────────

    private ScheduleItem buildScheduleItem(ScheduleItemCreateRequest req, Schedule schedule) {
        return ScheduleItem.builder()
                .schedule(schedule)
                .day(req.day())
                .orderIndex(req.orderIndex())
                .visitTime(req.visitTime())
                .category(req.category())
                .name(req.name())
                .address(req.address())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .memo(req.memo())
                .deepLinkUrl(req.deepLinkUrl())
                .estimatedCost(req.estimatedCost())
                .build();
    }
}
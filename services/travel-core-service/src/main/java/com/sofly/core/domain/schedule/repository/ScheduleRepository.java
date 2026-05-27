package com.sofly.core.domain.schedule.repository;

import com.sofly.core.domain.schedule.dto.ScheduleSummaryResponse;
import com.sofly.core.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    //워크스페이스별 일정 목록
    List<Schedule> findByWorkspaceIdOrderByVersionDesc(Long workspaceId);

    //워크스페이스의 최신 버전 일정
    Optional<Schedule> findTopByWorkspaceIdOrderByVersionDesc(Long workspaceId);

    // 워크스페이스의 최대 버전 번호 (새 버전 생성 시 사용)
    @Query("SELECT COALESCE(MAX(s.version), 0) FROM Schedule s WHERE s.workspace.id = :workspaceId")
    Integer findMaxVersionByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // items 패치 조인 (N+1 방지)
    @Query("SELECT s FROM Schedule s LEFT JOIN FETCH s.items WHERE s.id = :scheduleId")
    Optional<Schedule> findByIdWithItems(@Param("scheduleId") Long scheduleId);

    // 워크스페이스에 일정이 존재하는지 확인
    boolean existsByWorkspaceId(Long workspaceId);

    // workspaceId로 scheduleSummary 가져오기
    @Query("SELECT new com.sofly.core.domain.schedule.dto.ScheduleSummaryResponse(s.id, s.title, s.version, size(s.items), s.createdAt) FROM Schedule s WHERE s.workspace.id = :workspaceId ORDER BY s.version DESC")
    List<ScheduleSummaryResponse> findSummariesByWorkspaceId(@Param("workspaceId") Long workspaceId);
}

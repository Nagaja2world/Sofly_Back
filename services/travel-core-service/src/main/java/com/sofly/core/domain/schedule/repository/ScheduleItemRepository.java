package com.sofly.core.domain.schedule.repository;

import com.sofly.core.domain.schedule.entity.ScheduleItem.Category;
import com.sofly.core.domain.schedule.entity.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

    //itemId과 scheduleId로 items 찾기
    Optional<ScheduleItem> findByScheduleIdAndId(Long scheduleId, Long id);

    // 특정 일정의 모든 아이템 (day, orderIndex 정렬)
    List<ScheduleItem> findByScheduleIdOrderByDayAscOrderIndexAsc(Long scheduleId);

    // 특정 일차의 아이템만
    List<ScheduleItem> findByScheduleIdAndDayOrderByOrderIndexAsc(Long scheduleId, Integer day);

    // 특정 일차의 최대 orderIndex (새 아이템 추가 시 맨 뒤에 넣기)
    @Query("SELECT COALESCE(MAX(i.orderIndex), 0) FROM ScheduleItem i WHERE i.schedule.id = :scheduleId AND i.day = :day")
    Integer findMaxOrderIndexByScheduleIdAndDay(@Param("scheduleId") Long scheduleId, @Param("day") Integer day);

    // D&D 순서 변경: 특정 범위의 orderIndex를 일괄 +1 or -1 (같은 day 내 이동)
    @Modifying
    @Query("UPDATE ScheduleItem i SET i.orderIndex = i.orderIndex + :delta " +
            "WHERE i.schedule.id = :scheduleId AND i.day = :day " +
            "AND i.orderIndex BETWEEN :from AND :to")
    void shiftOrderIndex(@Param("scheduleId") Long scheduleId,
                         @Param("day") Integer day,
                         @Param("from") Integer from,
                         @Param("to") Integer to,
                         @Param("delta") int delta);

    // D&D 순서 변경: from 이후의 모든 orderIndex를 일괄 +1 or -1 (day 간 이동)
    @Modifying
    @Query("UPDATE ScheduleItem i SET i.orderIndex = i.orderIndex + :delta " +
            "WHERE i.schedule.id = :scheduleId AND i.day = :day " +
            "AND i.orderIndex >= :from")
    void shiftOrderIndexFrom(@Param("scheduleId") Long scheduleId,
                              @Param("day") Integer day,
                              @Param("from") Integer from,
                              @Param("delta") int delta);

    // 카테고리별 필터링
    List<ScheduleItem> findByScheduleIdAndCategoryOrderByDayAscOrderIndexAsc(Long scheduleId, Category category);

    // 딥링크 클릭 수 TOP N (통계용)
    @Query("SELECT i FROM ScheduleItem i WHERE i.schedule.id = :scheduleId AND i.deepLinkUrl IS NOT NULL ORDER BY i.deepLinkClickCount DESC LIMIT :limit")
    List<ScheduleItem> findTopDeepLinkItems(@Param("scheduleId") Long scheduleId, @Param("limit") int limit);

    // 일정 삭제 시 벌크 삭제 (orphanRemoval 대신 명시적으로 쓸 때)
    void deleteAllByScheduleId(Long scheduleId);
}

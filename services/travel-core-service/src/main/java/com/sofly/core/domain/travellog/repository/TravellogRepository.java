package com.sofly.core.domain.travellog.repository;

import com.sofly.core.domain.travellog.dto.TravellogSummaryResponse;
import com.sofly.core.domain.travellog.entity.TravelLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravellogRepository extends JpaRepository<TravelLog, Long> {

    /**
     * photos를 LEFT JOIN FETCH하면 Cartesian Product가 발생하므로,
     * SIZE() 서브쿼리로 photoCount만 집계하여 DTO로 직접 반환합니다.
     */
    @Query("""
            SELECT new com.sofly.core.domain.travellog.dto.TravellogSummaryResponse(
                t.id, t.day, t.travelDate, t.title, t.weather,
                SIZE(t.photos),
                t.createdAt
            )
            FROM TravelLog t
            WHERE t.workspace.id = :workspaceId
            ORDER BY t.travelDate ASC
            """)
    List<TravellogSummaryResponse> findAllSummaryByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT t FROM TravelLog t LEFT JOIN FETCH t.photos JOIN FETCH t.workspace JOIN FETCH t.author WHERE t.id = :id")
    Optional<TravelLog> findByIdWithPhotos(@Param("id") Long id);
}

package com.sofly.core.domain.travellog.repository;

import com.sofly.core.domain.travellog.entity.TravelLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravellogRepository extends JpaRepository<TravelLog, Long> {
    List<TravelLog> findByWorkspaceIdOrderByTravelDateAsc(Long workspaceId);

    @Query("SELECT t FROM TravelLog t LEFT JOIN FETCH t.photos WHERE t.id = :id")
    Optional<TravelLog> findByIdWithPhotos(@Param("id") Long id);
}

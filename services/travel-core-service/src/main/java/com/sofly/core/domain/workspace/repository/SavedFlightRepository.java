package com.sofly.core.domain.workspace.repository;

import com.sofly.core.domain.workspace.entity.SavedFlight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedFlightRepository extends JpaRepository<SavedFlight, Long> {

    List<SavedFlight> findAllByWorkspaceId(Long workspaceId);

    List<SavedFlight> findAllByWorkspaceIdAndFlightType(Long workspaceId, SavedFlight.FlightType flightType);

    Optional<SavedFlight> findByIdAndWorkspaceId(Long id, Long workspaceId);

    void deleteAllByWorkspaceId(Long workspaceId);
}

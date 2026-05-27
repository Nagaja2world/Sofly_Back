package com.sofly.core.domain.workspace.repository;

import com.sofly.core.domain.workspace.entity.SavedPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {
    List<SavedPlace> findAllByWorkspaceId(Long workspaceId);

    boolean existsByWorkspaceIdAndPlaceId(Long workspaceId, String placeId);

    Optional<SavedPlace> findByIdAndWorkspaceId(Long id, Long workspaceId);

    void deleteAllByWorkspaceId(Long workspaceId);
}

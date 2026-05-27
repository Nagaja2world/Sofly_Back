package com.sofly.core.domain.album.repository;

import com.sofly.core.domain.album.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    Optional<Album> findByWorkspaceId(Long workspaceId);

    void deleteByWorkspaceId(Long workspaceId);
}

package com.sofly.core.domain.album.repository;

import com.sofly.core.domain.album.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByAlbumIdOrderByCreatedAtDesc(Long albumId);
}

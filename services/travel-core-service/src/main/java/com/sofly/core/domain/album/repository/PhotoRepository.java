package com.sofly.core.domain.album.repository;

import com.sofly.core.domain.album.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    /** N+1 방지: uploadedBy를 JOIN FETCH로 한 번에 로드 */
    @Query("SELECT p FROM Photo p JOIN FETCH p.uploadedBy WHERE p.album.id = :albumId ORDER BY p.createdAt DESC")
    List<Photo> findByAlbumIdWithUploaderOrderByCreatedAtDesc(@Param("albumId") Long albumId);

    /** 권한 확인 및 상세 조회용: uploader, album, workspace를 한 번에 로드 */
    @Query("SELECT p FROM Photo p JOIN FETCH p.uploadedBy JOIN FETCH p.album a JOIN FETCH a.workspace WHERE p.id = :photoId")
    Optional<Photo> findByIdWithDetails(@Param("photoId") Long photoId);
}

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

    /** 사진이 속한 워크스페이스 ID 직접 조회 (연쇄 Lazy 로딩 방지) */
    @Query("SELECT p.album.workspace.id FROM Photo p WHERE p.id = :photoId")
    Optional<Long> findWorkspaceIdByPhotoId(@Param("photoId") Long photoId);

    /** 삭제 권한 확인용: uploadedBy를 JOIN FETCH로 함께 로드 */
    @Query("SELECT p FROM Photo p JOIN FETCH p.uploadedBy WHERE p.id = :photoId")
    Optional<Photo> findByIdWithUploader(@Param("photoId") Long photoId);
}

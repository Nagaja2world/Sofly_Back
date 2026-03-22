package com.sofly.core.domain.album.entity;

import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "albums")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Album extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false, unique = true)
    private Workspace workspace;

    // Google Drive 연동 정보
    private String driveFolderId;       // 연동된 Drive 폴더 ID
    private String driveFolderName;     // 폴더 이름

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Photo> photos = new ArrayList<>();

    public void connectDrive(String folderId, String folderName) {
        this.driveFolderId = folderId;
        this.driveFolderName = folderName;
    }

    public boolean isDriveConnected() {
        return this.driveFolderId != null;
    }
}

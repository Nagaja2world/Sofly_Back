package com.sofly.core.domain.workspace.entity;

import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "saved_place")
public class SavedPlace extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String placeId;

    @Column(nullable = false)
    private String name;

    private String address;
    private Double latitude;
    private Double longitude;
    private String primaryType;
    private String photoReference;
    private Double rating;
    private String googleMapsUri;
}

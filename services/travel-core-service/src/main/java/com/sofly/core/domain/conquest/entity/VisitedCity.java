package com.sofly.core.domain.conquest.entity;

import com.sofly.core.domain.conquest.enums.VisitStatus;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "visited_cities",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "city_name", "country_code"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class VisitedCity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String cityName;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VisitStatus status = VisitStatus.PLANNED;

    @Column(nullable = false)
    @Builder.Default
    private int visitCount = 0;

    public void updateStatus(VisitStatus status) {
        this.status = status;
        if (status == VisitStatus.VISITED) {
            this.visitCount++;
        }
    }
}

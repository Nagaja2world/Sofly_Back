package com.sofly.core.domain.conquest.entity;

import com.sofly.core.domain.conquest.enums.Continent;
import com.sofly.core.domain.conquest.enums.VisitStatus;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "visited_countries",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "country_code"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class VisitedCountry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 2)
    private String countryCode;     // ISO 3166-1 alpha-2

    @Column(nullable = false)
    private String countryName;     // 한국어 국가명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VisitStatus status = VisitStatus.UNVISITED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Continent continent;

    @Column(nullable = false)
    @Builder.Default
    private int visitCount = 0;

    public void updateStatus(VisitStatus status) {
        this.status = status;
        if (status == VisitStatus.VISITED) {
            this.visitCount++;
        }
    }

    public void incrementVisitCount() {
        this.visitCount++;
    }
}

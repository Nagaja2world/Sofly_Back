package com.sofly.core.domain.workspace.entity;

import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_flights")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SavedFlight extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    // supply 서비스에서 받아온 항공편 정보를 역정규화해서 저장
    @Column(nullable = false)
    private String flightNumber;        // KE123

    @Column(nullable = false)
    private String airline;             // 대한항공

    @Column(nullable = false)
    private String departureAirport;    // ICN

    @Column(nullable = false)
    private String arrivalAirport;      // NRT

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    private String duration;            // 2h 30m

    private Integer price;              // 참고용 가격

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FlightType flightType = FlightType.OUTBOUND;

    public enum FlightType { OUTBOUND, RETURN }
}

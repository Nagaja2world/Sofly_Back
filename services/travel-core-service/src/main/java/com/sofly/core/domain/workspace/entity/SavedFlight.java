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

    // ── 항공편 기본 정보 ──────────────────────────────
    @Column(nullable = false)
    private String flightNumber;        // MM809

    @Column(nullable = false)
    private String airline;             // Peach Aviation

    @Column(columnDefinition = "TEXT")
    private String airlineLogo;         // https://r-xx.bstatic.com/data/airlines_logo/MM.png

    @Column
    private String planeType;           // 333, 359 등

    @Column
    private String cabinClass;          // ECONOMY, BUSINESS

    // ── 공항 정보 ──────────────────────────────────────
    @Column(nullable = false)
    private String departureAirport;    // ICN (코드)

    @Column
    private String departureCity;       // Seoul

    @Column
    private String departureCountry;    // South Korea

    @Column
    private String departureTerminal;   // "2"

    @Column(nullable = false)
    private String arrivalAirport;      // HND

    @Column
    private String arrivalCity;         // Tokyo

    @Column
    private String arrivalCountry;      // Japan

    @Column
    private String arrivalTerminal;     // "3"

    // ── 시간 ──────────────────────────────────────────
    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @Column
    private Integer durationMinutes;    // String 말고 분 단위 Integer로

    // ── 가격 (분리 저장) ───────────────────────────────
    @Column
    private Integer totalPrice;         // 총액

    @Column
    private Integer baseFare;           // 운임

    @Column
    private Integer tax;                // 세금

    @Column
    private Integer platformFee;        // 플랫폼 수수료 (bcomMargin)

    @Column(length = 3)
    private String currencyCode;        // KRW

    // ── 수하물 ─────────────────────────────────────────
    @Column
    private Integer checkedBaggageKg;   // 23 (kg), null이면 미포함

    @Column
    private Integer checkedBaggagePiece;// 1

    @Column
    private Integer cabinBaggageKg;     // 10

    @Column
    private Boolean personalItemIncluded; // true/false

    // ── 예약 관련 ──────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String bookingToken;        // 예약시 필요한 token (길어서 TEXT)

    @Column
    private String offerReference;      // d6a1f_3212210372

    // ── 타입 ──────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FlightType flightType = FlightType.OUTBOUND;

    public enum FlightType { OUTBOUND, RETURN }
}

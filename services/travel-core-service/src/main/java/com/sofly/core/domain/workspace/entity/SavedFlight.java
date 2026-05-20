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

    @Column(columnDefinition = "TEXT")
    private String offerReference;      // d6a1f_3212210372

    // ── 타입 ──────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FlightType flightType = FlightType.OUTBOUND;

    public enum FlightType { OUTBOUND, RETURN }

    public void update(
            String flightNumber, String airline, String airlineLogo, String planeType, String cabinClass,
            String departureAirport, String departureCity, String departureCountry, String departureTerminal,
            String arrivalAirport, String arrivalCity, String arrivalCountry, String arrivalTerminal,
            LocalDateTime departureTime, LocalDateTime arrivalTime, Integer durationMinutes,
            Integer totalPrice, Integer baseFare, Integer tax, Integer platformFee, String currencyCode,
            Integer checkedBaggageKg, Integer checkedBaggagePiece, Integer cabinBaggageKg,
            Boolean personalItemIncluded, String bookingToken, String offerReference, FlightType flightType) {
        if (flightNumber != null) this.flightNumber = flightNumber;
        if (airline != null) this.airline = airline;
        if (airlineLogo != null) this.airlineLogo = airlineLogo;
        if (planeType != null) this.planeType = planeType;
        if (cabinClass != null) this.cabinClass = cabinClass;
        if (departureAirport != null) this.departureAirport = departureAirport;
        if (departureCity != null) this.departureCity = departureCity;
        if (departureCountry != null) this.departureCountry = departureCountry;
        if (departureTerminal != null) this.departureTerminal = departureTerminal;
        if (arrivalAirport != null) this.arrivalAirport = arrivalAirport;
        if (arrivalCity != null) this.arrivalCity = arrivalCity;
        if (arrivalCountry != null) this.arrivalCountry = arrivalCountry;
        if (arrivalTerminal != null) this.arrivalTerminal = arrivalTerminal;
        if (departureTime != null) this.departureTime = departureTime;
        if (arrivalTime != null) this.arrivalTime = arrivalTime;
        if (durationMinutes != null) this.durationMinutes = durationMinutes;
        if (totalPrice != null) this.totalPrice = totalPrice;
        if (baseFare != null) this.baseFare = baseFare;
        if (tax != null) this.tax = tax;
        if (platformFee != null) this.platformFee = platformFee;
        if (currencyCode != null) this.currencyCode = currencyCode;
        if (checkedBaggageKg != null) this.checkedBaggageKg = checkedBaggageKg;
        if (checkedBaggagePiece != null) this.checkedBaggagePiece = checkedBaggagePiece;
        if (cabinBaggageKg != null) this.cabinBaggageKg = cabinBaggageKg;
        if (personalItemIncluded != null) this.personalItemIncluded = personalItemIncluded;
        if (bookingToken != null) this.bookingToken = bookingToken;
        if (offerReference != null) this.offerReference = offerReference;
        if (flightType != null) this.flightType = flightType;
    }
}

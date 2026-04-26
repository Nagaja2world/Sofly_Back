// package com.sofly.core.domain.conquest.event;
//
// import lombok.Getter;
// import lombok.RequiredArgsConstructor;
//
// import java.time.LocalDateTime;
// import java.util.List;
//
// /**
//  * 항공편 저장 시 발행되는 이벤트.
//  * 워크스페이스 멤버 전원의 정복 지도에 도착 국가/도시를 PLANNED 상태로 반영한다.
//  */
// @Getter
// @RequiredArgsConstructor
// public class FlightSavedEvent {
//
//     private final List<Long> memberUserIds;   // 워크스페이스 멤버 userId 목록
//     private final Long workspaceId;
//     private final String departureAirport;    // IATA
//     private final String arrivalAirport;      // IATA
//     private final LocalDateTime departureTime;
// }

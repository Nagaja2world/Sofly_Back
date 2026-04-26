package com.sofly.core.global.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FlightSavedMessage {
    private Long workspaceId;
    private List<Long> memberUserIds;
    private String departureAirport;
    private String arrivalAirport;
    private LocalDateTime departureTime;
}

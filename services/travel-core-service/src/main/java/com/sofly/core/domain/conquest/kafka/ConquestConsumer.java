package com.sofly.core.domain.conquest.kafka;

import com.sofly.core.domain.conquest.service.AirportInfoService;
import com.sofly.core.domain.conquest.service.AirportInfoService.AirportInfo;
import com.sofly.core.domain.conquest.service.ConquestMapService;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.global.kafka.dto.FlightSavedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConquestConsumer {

    private final ConquestMapService conquestMapService;
    private final AirportInfoService airportInfoService;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;  // Spring Boot 자동 등록 빈

    private static final String FLIGHT_DEPARTURES_KEY = "flight:departures";

    @KafkaListener(topics = "flight.saved", groupId = "travel-core-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(FlightSavedMessage message) {
        log.info("flight.saved 수신: workspaceId={}, arrival={}", message.getWorkspaceId(), message.getArrivalAirport());

        AirportInfo arrivalInfo = airportInfoService.findByIata(message.getArrivalAirport()).orElse(null);
        if (arrivalInfo == null) {
            log.warn("알 수 없는 도착 공항 코드: {}", message.getArrivalAirport());
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        for (Long userId : message.getMemberUserIds()) {
            try {
                User user = userRepository.getReferenceById(userId);

                // 1. 도착 국가/도시 PLANNED 반영
                conquestMapService.applyPlannedStatus(user, arrivalInfo);

                // 2. 출발 시간 분기
                if (message.getDepartureTime().isBefore(now)) {
                    // 이미 출발한 항공편 → 즉시 VISITED 전환
                    conquestMapService.promoteToVisited(userId, arrivalInfo.countryCode());
                    log.info("즉시 VISITED 전환: userId={}, country={}", userId, arrivalInfo.countryCode());
                } else {
                    // 미래 출발 → Redis Sorted Set 등록 (score = 출발 epoch seconds)
                    long epochSeconds = message.getDepartureTime().toEpochSecond(ZoneOffset.UTC);
                    // member 포맷: {userId}:{countryCode}:{cityName}
                    // cityName은 한국어(콜론 없음)이므로 파싱 안전
                    String member = userId + ":" + arrivalInfo.countryCode() + ":" + arrivalInfo.cityName();
                    stringRedisTemplate.opsForZSet().add(FLIGHT_DEPARTURES_KEY, member, epochSeconds);
                    log.info("Redis 등록: member={}, score={}", member, epochSeconds);
                }
            } catch (Exception e) {
                log.error("ConquestConsumer 처리 실패: userId={}, err={}", userId, e.getMessage(), e);
            }
        }
    }
}

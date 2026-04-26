package com.sofly.core.domain.conquest.scheduler;

import com.sofly.core.domain.conquest.service.ConquestMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFlightDepartureScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final ConquestMapService conquestMapService;

    private static final String KEY = "flight:departures";

    @Scheduled(fixedDelay = 60_000)
    public void processDeadlines() {
        double nowScore = Instant.now().getEpochSecond();

        Set<String> due = stringRedisTemplate.opsForZSet().rangeByScore(KEY, 0, nowScore);
        if (due == null || due.isEmpty()) return;

        log.info("출발 도래 항목 {}건 처리 시작", due.size());

        for (String member : due) {
            // member 포맷: {userId}:{countryCode}:{cityName}
            // cityName에 ':' 없다고 보장되므로 최대 3개 토큰으로 split
            String[] parts = member.split(":", 3);
            if (parts.length < 2) {
                log.warn("잘못된 member 포맷, 스킵: {}", member);
                stringRedisTemplate.opsForZSet().remove(KEY, member);
                continue;
            }

            try {
                Long userId = Long.parseLong(parts[0]);
                String countryCode = parts[1];

                conquestMapService.promoteToVisited(userId, countryCode);
                stringRedisTemplate.opsForZSet().remove(KEY, member);
                log.info("PLANNED → VISITED 완료: userId={}, country={}", userId, countryCode);
            } catch (Exception e) {
                // 삭제하지 않음 → 다음 1분 주기에 재시도
                log.warn("promoteToVisited 실패 (재시도 예정): member={}, err={}", member, e.getMessage());
            }
        }
    }
}

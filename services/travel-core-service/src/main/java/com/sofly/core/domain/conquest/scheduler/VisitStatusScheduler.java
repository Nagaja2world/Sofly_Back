package com.sofly.core.domain.conquest.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sofly.core.domain.conquest.service.ConquestMapService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매일 자정에 PLANNED 상태의 국가/도시 중
 * 항공편 출발일이 지난 항목을 VISITED 로 자동 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisitStatusScheduler {

    private final ConquestMapService conquestMapService;

    // TODO: PLANNED status change to VISITED time setting 
    // 10초마다 실행으로 임시 변경
    // @Scheduled(fixedDelay = 10000)
    @Scheduled(fixedDelay = 14400000)  // 14400000ms = 4시간
    public void promoteToVisited() {
        log.info("[ConquestScheduler] PLANNED → VISITED 자동 전환 시작");
        conquestMapService.promotePlannedToVisited();
        log.info("[ConquestScheduler] 완료");
    }

    @PostConstruct
    public void init() {
        try {
            promoteToVisited();
        } catch (Exception e) {
            log.warn("[ConquestScheduler] 초기 실행 실패 - 다음 스케줄에서 재시도: {}", e.getMessage());
        }
    }
}

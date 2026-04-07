package com.sofly.core.domain.conquest.scheduler;

import com.sofly.core.domain.conquest.service.ConquestMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    @Scheduled(fixedDelay = 1800000)  // 1800000ms = 30분
    public void promoteToVisited() {
        log.info("[ConquestScheduler] PLANNED → VISITED 자동 전환 시작");
        conquestMapService.promotePlannedToVisited();
        log.info("[ConquestScheduler] 완료");
    }
}

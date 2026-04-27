package com.sofly.core.domain.workspace.kafka;

import com.sofly.core.global.kafka.dto.FlightSavedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WorkspaceConsumer {

    @KafkaListener(topics = "flight.saved", groupId = "travel-core-workspace",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(FlightSavedMessage message) {
        // TODO: 워크스페이스 일정 섹션 자동 업데이트 스펙 확정 후 구현
        log.info("[워크스페이스 stub] 일정 섹션 업데이트 대상: workspaceId={}", message.getWorkspaceId());
    }
}

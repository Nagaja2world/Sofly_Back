package com.sofly.core.domain.workspace.kafka;

import com.sofly.core.global.kafka.dto.FlightSavedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer {

    @KafkaListener(topics = "flight.saved", groupId = "travel-core-notification",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(FlightSavedMessage message) {
        // TODO: FCM / 웹소켓 등 알림 인프라 구현 후 실제 발송 로직으로 교체
        log.info("[알림 stub] 항공편 저장 알림 대상: workspaceId={}, members={}",
                message.getWorkspaceId(), message.getMemberUserIds());
    }
}

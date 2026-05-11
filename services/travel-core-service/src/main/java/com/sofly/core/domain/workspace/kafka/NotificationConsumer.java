package com.sofly.core.domain.workspace.kafka;

import com.sofly.core.global.kafka.dto.FlightSavedMessage;
import com.sofly.core.global.kafka.dto.InvitationCreatedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer {

    @KafkaListener(topics = "flight.saved", groupId = "travel-core-notification",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeFlightSaved(FlightSavedMessage message) {
        // TODO: FCM / 웹소켓 등 알림 인프라 구현 후 실제 발송 로직으로 교체
        log.info("[알림 stub] 항공편 저장 알림 대상: workspaceId={}, members={}",
                message.getWorkspaceId(), message.getMemberUserIds());
    }

    @KafkaListener(topics = "workspace.invitation", groupId = "travel-core-notification-invitation",
            containerFactory = "invitationListenerContainerFactory")
    public void consumeInvitation(InvitationCreatedMessage message) {
        // TODO: FCM / 웹소켓 등 알림 인프라 구현 후 실제 발송 로직으로 교체
        log.info("[알림 stub] 워크스페이스 초대 알림: invitationId={}, workspaceId={}, inviteeId={}, inviter={}",
                message.getInvitationId(), message.getWorkspaceId(),
                message.getInviteeId(), message.getInviterNickname());
    }
}

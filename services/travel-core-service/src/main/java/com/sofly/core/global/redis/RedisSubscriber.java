package com.sofly.core.global.redis;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofly.core.domain.messaging.document.MessagingMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            MessagingMessage msg = objectMapper.readValue(
                    message.getBody(), MessagingMessage.class);
            // ✅ 실제 WebSocket 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/sub/chat/" + msg.getMessagingRoomId(), msg);
        } catch (Exception e) {
            log.error("Redis 메시지 브로드캐스트 실패: {}", e.getMessage());
        }
    }
}

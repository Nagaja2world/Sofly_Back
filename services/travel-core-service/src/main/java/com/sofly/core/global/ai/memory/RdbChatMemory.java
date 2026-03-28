package com.sofly.core.global.ai.memory;

import com.sofly.core.domain.chat.entity.ChatMessage;
import com.sofly.core.domain.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RdbChatMemory implements ChatMemory {

    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "chat:memory:";
    private static final Duration TTL = Duration.ofHours(24);
    private static final int WINDOW_SIZE = 20;

    @Override
    public void add(String conversationId, List<Message> messages) {
        Long chatRoomId = parseRoomId(conversationId);

        messages.forEach(message -> {
            ChatMessage.Role role = message.getMessageType() == MessageType.USER
                    ? ChatMessage.Role.USER
                    : ChatMessage.Role.ASSISTANT;

            chatMessageRepository.save(ChatMessage.builder()
                    .chatRoomId(chatRoomId)
                    .role(role)
                    .content(message.getText())
                    .build());
        });

        redisTemplate.delete(CACHE_PREFIX + conversationId);
        log.debug("ChatMemory saved - chatRoomId: {}", chatRoomId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Message> get(String conversationId) {
        String cacheKey = CACHE_PREFIX + conversationId;
        Long chatRoomId = parseRoomId(conversationId);

        try {
            List<Message> cached = (List<Message>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("ChatMemory cache hit - chatRoomId: {}", chatRoomId);
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis 조회 실패, DB로 fallback - chatRoomId: {}", chatRoomId);
        }

        List<ChatMessage> dbMessages = chatMessageRepository
                .findTopNByChatRoomIdOrderByCreatedAtDesc(chatRoomId, WINDOW_SIZE);
        Collections.reverse(dbMessages);

        List<Message> messages = dbMessages.stream()
                .map(m -> m.getRole() == ChatMessage.Role.USER
                        ? (Message) new UserMessage(m.getContent())
                        : (Message) new AssistantMessage(m.getContent()))
                .toList();

        try {
            redisTemplate.opsForValue().set(cacheKey, messages, TTL);
        } catch (Exception e) {
            log.warn("Redis 저장 실패 - chatRoomId: {}", chatRoomId);
        }

        return messages;
    }

    @Override
    public void clear(String conversationId) {
        Long chatRoomId = parseRoomId(conversationId);
        chatMessageRepository.deleteByChatRoomId(chatRoomId);
        redisTemplate.delete(CACHE_PREFIX + conversationId);
        log.debug("ChatMemory cleared - chatRoomId: {}", chatRoomId);
    }

    // "room:1" → 1L
    private Long parseRoomId(String conversationId) {
        return Long.parseLong(conversationId.replace("room:", ""));
    }
}

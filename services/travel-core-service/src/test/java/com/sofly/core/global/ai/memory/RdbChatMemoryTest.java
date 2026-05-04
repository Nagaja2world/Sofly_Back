package com.sofly.core.global.ai.memory;

import com.sofly.core.domain.chat.entity.ChatMessage;
import com.sofly.core.domain.chat.repository.ChatMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RdbChatMemoryTest {

    @Mock
    ChatMessageRepository chatMessageRepository;

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ValueOperations<String, Object> valueOperations;

    @Test
    @DisplayName("USER/ASSISTANT 메시지만 DB에 저장하고 Redis 캐시를 무효화한다")
    @SuppressWarnings("unchecked")
    void add_persistsOnlyUserAndAssistantMessages() {
        RdbChatMemory memory = new RdbChatMemory(chatMessageRepository, redisTemplate);
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);

        memory.add("room:7", List.of(
                new SystemMessage("system prompt"),
                new UserMessage("사용자 질문"),
                new AssistantMessage("AI 답변")
        ));

        verify(chatMessageRepository).saveAll(captor.capture());
        verify(redisTemplate).delete("chat:memory:room:7");

        List<ChatMessage> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(ChatMessage::getChatRoomId).containsOnly(7L);
        assertThat(saved).extracting(ChatMessage::getRole)
                .containsExactly(ChatMessage.Role.USER, ChatMessage.Role.ASSISTANT);
        assertThat(saved).extracting(ChatMessage::getContent)
                .containsExactly("사용자 질문", "AI 답변");
    }

    @Test
    @DisplayName("저장 가능한 메시지가 없으면 DB 저장 없이 캐시만 무효화한다")
    void add_skipsEmptyPersistableMessages() {
        RdbChatMemory memory = new RdbChatMemory(chatMessageRepository, redisTemplate);

        memory.add("room:7", List.of(new SystemMessage("system prompt")));

        verifyNoInteractions(chatMessageRepository);
        verify(redisTemplate).delete("chat:memory:room:7");
    }

    @Test
    @DisplayName("텍스트를 제공하지 않는 ASSISTANT 메시지는 저장하지 않는다")
    void add_skipsAssistantMessageWithoutText() {
        RdbChatMemory memory = new RdbChatMemory(chatMessageRepository, redisTemplate);
        Message message = mock(Message.class);
        given(message.getMessageType()).willReturn(org.springframework.ai.chat.messages.MessageType.ASSISTANT);
        willThrow(new UnsupportedOperationException()).given(message).getText();

        memory.add("room:7", List.of(message));

        verifyNoInteractions(chatMessageRepository);
        verify(redisTemplate).delete("chat:memory:room:7");
    }

    @Test
    @DisplayName("Redis miss면 DB 최근 메시지를 오래된 순서로 복원하고 캐시한다")
    void get_loadsFromDbAndCachesOnRedisMiss() {
        RdbChatMemory memory = new RdbChatMemory(chatMessageRepository, redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("chat:memory:room:7")).willReturn(null);
        given(chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(eq(7L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        chatMessage(ChatMessage.Role.ASSISTANT, "두 번째"),
                        chatMessage(ChatMessage.Role.USER, "첫 번째")
                )));

        List<Message> messages = memory.get("room:7");

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(0).getText()).isEqualTo("첫 번째");
        assertThat(messages.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(messages.get(1).getText()).isEqualTo("두 번째");
        verify(valueOperations).set(eq("chat:memory:room:7"), any(), any());
    }

    private ChatMessage chatMessage(ChatMessage.Role role, String content) {
        return ChatMessage.builder()
                .chatRoomId(7L)
                .role(role)
                .content(content)
                .build();
    }
}

package com.sofly.core.domain.chat.service;

import com.sofly.core.domain.chat.dto.*;
import com.sofly.core.domain.chat.entity.ChatMessage;
import com.sofly.core.domain.chat.entity.ChatRoom;
import com.sofly.core.domain.chat.repository.ChatMessageRepository;
import com.sofly.core.domain.chat.repository.ChatRoomRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import com.sofly.core.global.ai.memory.RdbChatMemory;
import com.sofly.core.global.ai.tools.PlaceVerificationTools;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int LAST_MESSAGE_PREVIEW_LENGTH = 100;

    private final ChatClient chatClient;
    private final RdbChatMemory rdbChatMemory;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PlaceVerificationTools placeVerificationTools;

    // ChatRoom 생성 (Workspace당 여러 개 가능)
    @Transactional
    public ChatRoomSummaryResponse createChatRoom(ChatRoomCreateRequest request) {
        workspaceRepository.findById(request.workspaceId())
                .orElseThrow(() -> new EntityNotFoundException("Workspace not found: " + request.workspaceId()));

        ChatRoom room = ChatRoom.builder()
                .workspaceId(request.workspaceId())
                .title(request.title() != null ? request.title() : "새 여행 계획")
                .build();

        return ChatRoomSummaryResponse.from(chatRoomRepository.save(room));
    }

    // 메시지 전송 및 AI 응답
    @Transactional
    public ChatResponse chat(Long roomId, ChatRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("ChatRoom not found: " + roomId));

        String conversationId = "room:" + roomId;

        String response = chatClient.prompt()
                .user(request.message())
                .advisors(MessageChatMemoryAdvisor.builder(rdbChatMemory)
                        .conversationId(conversationId)
                        .build())
                .tools(placeVerificationTools)
                .call()
                .content();

        room.updateLastMessage(response.length() > LAST_MESSAGE_PREVIEW_LENGTH ? response.substring(0, LAST_MESSAGE_PREVIEW_LENGTH) + "…" : response);

        return new ChatResponse(
                roomId,
                response,
                ChatMessage.Role.ASSISTANT,
                LocalDateTime.now()
        );
    }

    // 워크스페이스 소속 ChatRoom 목록 (왼쪽 탭용)
    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> getChatRooms(Long workspaceId) {
        return chatRoomRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(ChatRoomSummaryResponse::from)
                .toList();
    }

    // 특정 ChatRoom의 메시지 전체 (채팅창 진입 시)
    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatRoomMessages(Long roomId) {
        chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("ChatRoom not found: " + roomId));

        return new ChatHistoryResponse(
                roomId,
                chatMessageRepository
                        .findByChatRoomIdOrderByCreatedAtAsc(roomId)
                        .stream()
                        .map(ChatHistoryResponse.MessageDto::from)
                        .toList()
        );
    }
}

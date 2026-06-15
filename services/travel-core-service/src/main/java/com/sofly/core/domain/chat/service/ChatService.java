package com.sofly.core.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofly.core.domain.chat.dto.*;
import com.sofly.core.domain.chat.entity.ChatMessage;
import com.sofly.core.domain.chat.entity.ChatRoom;
import com.sofly.core.domain.chat.repository.ChatMessageRepository;
import com.sofly.core.domain.chat.repository.ChatRoomRepository;
import com.sofly.core.domain.schedule.dto.ScheduleCreateRequest;
import com.sofly.core.domain.schedule.dto.ScheduleItemCreateRequest;
import com.sofly.core.domain.schedule.dto.ScheduleResponse;
import com.sofly.core.domain.schedule.service.ScheduleService;
import com.sofly.core.domain.workspace.code.WorkspaceErrorCode;
import com.sofly.core.domain.workspace.entity.WorkspaceMember;
import com.sofly.core.domain.workspace.entity.WorkspaceMember.MemberRole;
import com.sofly.core.domain.workspace.exception.WorkspaceException;
import com.sofly.core.domain.workspace.repository.WorkspaceMemberRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import com.sofly.core.global.ai.memory.RdbChatMemory;
import com.sofly.core.global.security.util.SecurityUtils;
import com.sofly.core.global.ai.tools.PlaceVerificationTools;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int LAST_MESSAGE_PREVIEW_LENGTH = 100;

    private final ChatClient chatClient;
    private final RdbChatMemory rdbChatMemory;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final PlaceVerificationTools placeVerificationTools;
    private final ScheduleService scheduleService;
    private final ObjectMapper objectMapper;

    // ChatRoom 생성 (Workspace당 여러 개 가능)
    @Transactional
    public ChatRoomSummaryResponse createChatRoom(ChatRoomCreateRequest request) {
        workspaceRepository.findById(request.workspaceId())
                .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_NOT_FOUND));
        requireMember(request.workspaceId());

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
                .orElseThrow(() -> new SoflyException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        requireMember(room.getWorkspaceId());

        String conversationId = "room:" + roomId;

        log.info("[AI-CHAT] ▶ USER  roomId={} | message=\"{}\"", roomId, request.message());

        long start = System.currentTimeMillis();
        String response = chatClient.prompt()
                .user(request.message())
                .advisors(MessageChatMemoryAdvisor.builder(rdbChatMemory)
                        .conversationId(conversationId)
                        .build())
                .tools(placeVerificationTools)
                .call()
                .content();
        long elapsed = System.currentTimeMillis() - start;

        log.info("[AI-CHAT] ◀ GEMINI roomId={} | elapsed={}ms | response=\"{}\"",
                roomId, elapsed,
                response.length() > 300 ? response.substring(0, 300) + "…" : response);

        room.updateLastMessage(response.length() > LAST_MESSAGE_PREVIEW_LENGTH ? response.substring(0, LAST_MESSAGE_PREVIEW_LENGTH) + "…" : response);

        return new ChatResponse(
                roomId,
                response,
                ChatMessage.Role.ASSISTANT,
                LocalDateTime.now()
        );
    }

    // ChatRoom 제목 수정
    @Transactional
    public ChatRoomSummaryResponse updateChatRoomTitle(Long roomId, ChatRoomUpdateRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new SoflyException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        requireMember(room.getWorkspaceId());
        room.updateTitle(request.title());
        return ChatRoomSummaryResponse.from(room);
    }

    // ChatRoom 삭제 (메시지 + AI 메모리 포함)
    @Transactional
    public void deleteChatRoom(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new SoflyException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        requireMember(room.getWorkspaceId());
        rdbChatMemory.clear("room:" + roomId);
        chatRoomRepository.deleteById(roomId);
    }

    // 워크스페이스 소속 ChatRoom 목록 (왼쪽 탭용)
    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> getChatRooms(Long workspaceId) {
        requireMember(workspaceId);
        return chatRoomRepository.findByWorkspaceIdOrderByIdDesc(workspaceId)
                .stream()
                .map(ChatRoomSummaryResponse::from)
                .toList();
    }

    // 메시지 스트리밍 (SSE용) — 청크 단위 Flux<String> 반환
    public Flux<String> chatStream(Long roomId, ChatRequest request) {
        ChatRoom streamRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new SoflyException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        requireMember(streamRoom.getWorkspaceId());

        String conversationId = "room:" + roomId;

        return chatClient.prompt()
                .user(request.message())
                .advisors(MessageChatMemoryAdvisor.builder(rdbChatMemory)
                        .conversationId(conversationId)
                        .build())
                .tools(placeVerificationTools)
                .stream()
                .content();
    }

    // 스트리밍 완료 후 ChatRoom 마지막 메시지 업데이트 (컨트롤러 onComplete 콜백에서 호출)
    @Transactional
    public void finalizeStream(Long roomId, String fullResponse) {
        chatRoomRepository.findById(roomId).ifPresent(room ->
                room.updateLastMessage(fullResponse.length() > LAST_MESSAGE_PREVIEW_LENGTH
                        ? fullResponse.substring(0, LAST_MESSAGE_PREVIEW_LENGTH) + "…"
                        : fullResponse)
        );
    }

    // AI 확정 JSON → Schedule 저장
    @Transactional
    public ScheduleResponse saveScheduleFromChat(Long roomId, Long workspaceId) {
        requireMember(workspaceId);
        ChatMessage lastAssistantMessage = chatMessageRepository
                .findTopByChatRoomIdAndRoleOrderByCreatedAtDesc(roomId, ChatMessage.Role.ASSISTANT)
                .orElseThrow(() -> new SoflyException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        AiScheduleOutput output;
        try {
            String content = lastAssistantMessage.getContent();
            log.info("[AI-JSON] 원본 응답 길이={}자 | 앞 200자=\"{}\"",
                    content.length(), content.substring(0, Math.min(200, content.length())));

            // AI가 마크다운 코드 블록(```json ... ```)이나 앞뒤 텍스트를 포함할 경우 JSON만 추출
            if (content.contains("```json")) {
                content = content.substring(content.indexOf("```json") + 7, content.lastIndexOf("```")).trim();
                log.info("[AI-JSON] 마크다운 코드 블록 제거 완료");
            } else if (content.contains("{") && content.contains("}")) {
                content = content.substring(content.indexOf("{"), content.lastIndexOf("}") + 1).trim();
                log.info("[AI-JSON] 앞뒤 텍스트 제거 완료");
            }

            log.info("[AI-JSON] 파싱 시도 | JSON 앞 500자=\"{}\"",
                    content.substring(0, Math.min(500, content.length())));
            output = objectMapper.readValue(content, AiScheduleOutput.class);
            log.info("[AI-JSON] 파싱 성공 | days={}일", output.days() != null ? output.days().size() : 0);
        } catch (JsonProcessingException e) {
            log.error("[AI-JSON] 파싱 실패 | error={}", e.getMessage());
            throw new SoflyException(ErrorCode.INVALID_AI_RESPONSE);
        }

        if (output.days() == null || output.days().isEmpty()) {
            throw new SoflyException(ErrorCode.INVALID_AI_RESPONSE);
        }

        List<ScheduleItemCreateRequest> items = output.days().stream()
                .flatMap(day -> day.items().stream()
                        .map(item -> new ScheduleItemCreateRequest(
                                day.day(),
                                item.orderIndex(),
                                parseVisitTime(item.visitTime()),
                                item.category(),
                                item.name(),
                                item.address(),
                                item.latitude(),
                                item.longitude(),
                                item.placeId(),
                                item.photoReference(),
                                item.memo(),
                                item.deepLinkUrl(),
                                item.estimatedCost()
                        ))
                )
                .toList();

        return scheduleService.createSchedule(new ScheduleCreateRequest(workspaceId, null, items));
    }

    private LocalTime parseVisitTime(String visitTime) {
        if (visitTime == null) return null;
        try {
            return LocalTime.parse(visitTime);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** EDITOR 이상(OWNER/EDITOR)만 허용, VIEWER와 비멤버는 거부 */
    private void requireMember(Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_FORBIDDEN));
        if (member.getRole() == MemberRole.VIEWER) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_FORBIDDEN);
        }
    }

    // 특정 ChatRoom의 메시지 전체 (채팅창 진입 시)
    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatRoomMessages(Long roomId) {
        ChatRoom msgRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new SoflyException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        requireMember(msgRoom.getWorkspaceId());

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

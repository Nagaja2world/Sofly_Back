package com.sofly.core.domain.chat.controller;

import com.sofly.core.domain.chat.dto.*;
import com.sofly.core.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat", description = "AI 여행 플래너 채팅")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "ChatRoom 생성", description = "워크스페이스에 새 채팅방을 생성합니다.")
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomSummaryResponse> createChatRoom(
            @RequestBody @Valid ChatRoomCreateRequest request
    ) {
        return ResponseEntity.ok(chatService.createChatRoom(request));
    }

    @Operation(summary = "ChatRoom 목록 조회", description = "워크스페이스의 ChatRoom 목록(제목 + 마지막 메시지)을 반환합니다.")
    @GetMapping("/workspaces/{workspaceId}/rooms")
    public ResponseEntity<List<ChatRoomSummaryResponse>> getChatRooms(
            @PathVariable Long workspaceId
    ) {
        return ResponseEntity.ok(chatService.getChatRooms(workspaceId));
    }

    @Operation(summary = "ChatRoom 메시지 조회", description = "특정 ChatRoom의 전체 메시지를 반환합니다.")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatHistoryResponse> getChatRoomMessages(
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(chatService.getChatRoomMessages(roomId));
    }

    @Operation(summary = "메시지 전송", description = "ChatRoom에 메시지를 보내고 AI 응답을 받습니다.")
    @PostMapping("/rooms/{roomId}")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable Long roomId,
            @RequestBody @Valid ChatRequest request
    ) {
        return ResponseEntity.ok(chatService.chat(roomId, request));
    }
}

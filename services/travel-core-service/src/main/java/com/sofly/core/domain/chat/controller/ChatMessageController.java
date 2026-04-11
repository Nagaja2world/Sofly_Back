package com.sofly.core.domain.chat.controller;

import com.sofly.core.domain.chat.dto.ChatHistoryResponse;
import com.sofly.core.domain.chat.dto.ChatRequest;
import com.sofly.core.domain.chat.dto.ChatResponse;
import com.sofly.core.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat Message", description = "AI 채팅 메시지 전송·조회 API")
@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;

    @Operation(summary = "ChatRoom 메시지 조회", description = "특정 ChatRoom의 전체 메시지를 반환합니다.")
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ChatHistoryResponse> getChatRoomMessages(
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(chatService.getChatRoomMessages(roomId));
    }

    @Operation(summary = "메시지 전송", description = "ChatRoom에 메시지를 보내고 AI 응답을 받습니다.")
    @PostMapping("/{roomId}")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable Long roomId,
            @RequestBody @Valid ChatRequest request
    ) {
        return ResponseEntity.ok(chatService.chat(roomId, request));
    }
}

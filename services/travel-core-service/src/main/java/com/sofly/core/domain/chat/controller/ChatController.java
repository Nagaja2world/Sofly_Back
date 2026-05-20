package com.sofly.core.domain.chat.controller;

import com.sofly.core.domain.chat.dto.ChatRoomCreateRequest;
import com.sofly.core.domain.chat.dto.ChatRoomSummaryResponse;
import com.sofly.core.domain.chat.dto.ChatRoomUpdateRequest;
import com.sofly.core.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat Room", description = "AI 채팅방 생성·조회 API")
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

    @Operation(summary = "ChatRoom 제목 수정", description = "채팅방 제목을 수정합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "제목이 비어 있음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @PatchMapping("/rooms/{roomId}/title")
    public ResponseEntity<ChatRoomSummaryResponse> updateChatRoomTitle(
            @PathVariable Long roomId,
            @RequestBody @Valid ChatRoomUpdateRequest request) {
        return ResponseEntity.ok(chatService.updateChatRoomTitle(roomId, request));
    }

    @Operation(summary = "ChatRoom 삭제", description = "채팅방과 모든 메시지(AI 메모리 포함)를 삭제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable Long roomId) {
        chatService.deleteChatRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}

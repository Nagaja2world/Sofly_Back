package com.sofly.core.domain.messaging.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofly.core.domain.messaging.dto.MessagingMessageRequest;
import com.sofly.core.domain.messaging.dto.MessagingMessageResponse;
import com.sofly.core.domain.messaging.dto.MessagingRoomCreateRequest;
import com.sofly.core.domain.messaging.entity.MessagingRoom;
import com.sofly.core.domain.messaging.service.MessagingService;
import com.sofly.core.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Messaging", description = "실시간 채팅")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messaging")
public class MessagingController {

    private final MessagingService messagingService;

    @Operation(summary = "채팅방 생성", description = "채팅방을 생성합니다. (1:1, 그룹, 워크스페이스)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값이 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    })
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<MessagingRoom>> createRoom(
            @RequestBody @Valid MessagingRoomCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(messagingService.createRoom(request)));
    }

    @Operation(summary = "메시지 히스토리 조회", description = "채팅방의 메시지 목록을 시간 순으로 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<MessagingMessageResponse>>> getMessages(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(ApiResponse.success(messagingService.getMessages(roomId)));
    }

    @Operation(
        summary = "[WebSocket] 실시간 메시지 전송",
        description = """
            WebSocket STOMP 프로토콜을 사용하는 실시간 메시지 전송 엔드포인트입니다.
            REST API가 아니므로 아래 가이드를 참고하여 연결하세요.
            
            **연결 방법**
            - WebSocket 엔드포인트: `ws://localhost:8080/ws`
            - SockJS fallback 지원: `http://localhost:8080/ws`
            
            **메시지 발행 (송신)**
            - Destination: `/pub/chat.message.{roomId}`
            
            **메시지 구독 (수신)**
            - Destination: `/sub/chat/{roomId}`
            
            **요청 Body 예시**
```json
            {
              "senderId": 1,
              "senderNickname": "홍길동",
              "content": "안녕하세요!",
              "type": "TEXT"
            }
```
            
            **응답 Body 예시**
```json
            {
              "id": "664f1a2b3c4d5e6f7a8b9c0d",
              "messagingRoomId": 1,
              "senderId": 1,
              "senderNickname": "홍길동",
              "content": "안녕하세요!",
              "type": "TEXT",
              "createdAt": "2026-04-29T16:37:00"
            }
```
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 전송 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @GetMapping("/rooms/{roomId}/ws-docs")  // Swagger 노출용 더미 엔드포인트
    public ResponseEntity<Void> webSocketDocs(@PathVariable Long roomId) {
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/chat.message.{roomId}")
    @SendTo("/sub/chat/{roomId}")
    public MessagingMessageResponse sendMessage(
            @DestinationVariable Long roomId,
            MessagingMessageRequest request) {
        return messagingService.sendMessage(roomId, request);
    }
}

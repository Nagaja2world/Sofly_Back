package com.sofly.core.domain.chat.controller;

import com.sofly.core.domain.chat.dto.ChatHistoryResponse;
import com.sofly.core.domain.chat.dto.ChatRequest;
import com.sofly.core.domain.chat.dto.ChatResponse;
import com.sofly.core.domain.chat.service.ChatService;
import com.sofly.core.domain.schedule.dto.ScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Tag(name = "Chat Message", description = "AI 채팅 메시지 전송·조회 API")
@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatMessageController {

    // SSE heartbeat용 공용 스레드 풀 — 요청마다 생성하지 않고 애플리케이션 전체에서 공유
    private static final ScheduledExecutorService HEARTBEAT_SCHEDULER =
            Executors.newScheduledThreadPool(10);

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

    @Operation(
            summary = "메시지 전송 (스트리밍)",
            description = "ChatRoom에 메시지를 보내고 AI 응답을 SSE(text/event-stream)로 청크 단위 수신합니다. "
                    + "긴 일정 생성 시 체감 응답 속도가 개선됩니다."
    )
    @PostMapping(value = "/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @PathVariable Long roomId,
            @RequestBody @Valid ChatRequest request
    ) {
        SseEmitter emitter = new SseEmitter(180_000L);
        StringBuilder collector = new StringBuilder();

        // nginx/LB idle timeout 방지용 heartbeat (15초 간격)
        ScheduledFuture<?> heartbeat = HEARTBEAT_SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (IOException e) {
                Thread.currentThread().interrupt();
            }
        }, 15, 15, TimeUnit.SECONDS);

        emitter.onCompletion(() -> heartbeat.cancel(true));
        emitter.onError(t -> heartbeat.cancel(true));

        chatService.chatStream(roomId, request)
                .subscribe(
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event().data(chunk));
                                collector.append(chunk);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                            } catch (IOException ignored) {}
                            emitter.completeWithError(error);
                        },
                        () -> {
                            chatService.finalizeStream(roomId, collector.toString());
                            emitter.complete();
                        }
                );

        return emitter;
    }

    @Operation(
            summary = "AI 확정 일정 저장",
            description = "AI가 확정 JSON을 반환한 후 호출합니다. "
                    + "마지막 AI 메시지의 JSON을 파싱해 Schedule로 저장합니다."
    )
    @PostMapping("/{roomId}/save-schedule")
    public ResponseEntity<ScheduleResponse> saveSchedule(
            @PathVariable Long roomId,
            @Parameter(description = "저장할 워크스페이스 ID", required = true)
            @RequestParam Long workspaceId
    ) {
        return ResponseEntity.ok(chatService.saveScheduleFromChat(roomId, workspaceId));
    }
}

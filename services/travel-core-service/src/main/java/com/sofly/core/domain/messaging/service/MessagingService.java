package com.sofly.core.domain.messaging.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofly.core.domain.messaging.document.MessagingMessage;
import com.sofly.core.domain.messaging.dto.MessagingMessageRequest;
import com.sofly.core.domain.messaging.dto.MessagingMessageResponse;
import com.sofly.core.domain.messaging.dto.MessagingRoomCreateRequest;
import com.sofly.core.domain.messaging.entity.MessagingRoom;
import com.sofly.core.domain.messaging.entity.MessagingRoomMember;
import com.sofly.core.domain.messaging.repository.MessagingMessageRepository;
import com.sofly.core.domain.messaging.repository.MessagingRoomMemberRepository;
import com.sofly.core.domain.messaging.repository.MessagingRoomRepository;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private final MessagingRoomRepository messagingRoomRepository;
    private final MessagingRoomMemberRepository messagingRoomMemberRepository;
    private final MessagingMessageRepository messagingMessageRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;

    // 채팅방 생성
    @Transactional
    public MessagingRoom createRoom(MessagingRoomCreateRequest request) {
        MessagingRoom room = MessagingRoom.builder()
                .type(request.type())
                .name(request.name())
                .workspaceId(request.workspaceId())
                .build();

        MessagingRoom savedRoom = messagingRoomRepository.save(room);

        // 멤버 등록
        List<MessagingRoomMember> members = request.memberIds().stream()
                .map(userId -> MessagingRoomMember.builder()
                        .messagingRoom(savedRoom)
                        .userId(userId)
                        .build())
                .toList();

        messagingRoomMemberRepository.saveAll(members);

        return savedRoom;
    }

    // 메시지 전송 → MongoDB 저장 → Redis Pub/Sub 발행
    public MessagingMessageResponse sendMessage(
            Long roomId,
            MessagingMessageRequest request,
            Long senderId) {  // ← 파라미터로 받기

        messagingRoomRepository.findById(roomId)
            .orElseThrow(()  -> new SoflyException(ErrorCode.ROOM_NOT_FOUND));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new SoflyException(ErrorCode.USER_NOT_FOUND));

        MessagingMessage message = MessagingMessage.builder()
                .messagingRoomId(roomId)
                .senderId(senderId)
                .senderNickname(sender.getNickname())
                .content(request.content())
                .type(request.type())
                .createdAt(LocalDateTime.now())
                .build();

        MessagingMessage saved = messagingMessageRepository.save(message);
        redisTemplate.convertAndSend(channelTopic.getTopic(), saved);
        return MessagingMessageResponse.from(saved);
    }

    // 채팅방 메시지 히스토리 조회
    @Transactional(readOnly = true)
    public Page<MessagingMessageResponse> getMessages(Long roomId, Pageable pageable) {
        return messagingMessageRepository
                .findByMessagingRoomIdOrderByCreatedAtDesc(roomId, pageable)
                .map(MessagingMessageResponse::from);
    }
}

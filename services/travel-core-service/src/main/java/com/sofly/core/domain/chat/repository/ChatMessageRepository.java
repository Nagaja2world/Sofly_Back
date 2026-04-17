package com.sofly.core.domain.chat.repository;

import com.sofly.core.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 히스토리 전체 조회 (오래된 순)
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    // 컨텍스트용 최근 N개 조회
    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    // 특정 role의 가장 최근 메시지 조회 (일정 저장 시 마지막 AI 응답 추출용)
    Optional<ChatMessage> findTopByChatRoomIdAndRoleOrderByCreatedAtDesc(Long chatRoomId, ChatMessage.Role role);

    void deleteByChatRoomId(Long chatRoomId);
}

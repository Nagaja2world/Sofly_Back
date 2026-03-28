package com.sofly.core.domain.chat.repository;

import com.sofly.core.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 히스토리 전체 조회 (오래된 순)
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    // 컨텍스트용 최근 N개 조회
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.chatRoomId = :chatRoomId
        ORDER BY m.createdAt DESC
        LIMIT :limit
        """)
    List<ChatMessage> findTopNByChatRoomIdOrderByCreatedAtDesc(
            @Param("chatRoomId") Long chatRoomId,
            @Param("limit") int limit
    );

    void deleteByChatRoomId(Long chatRoomId);
}

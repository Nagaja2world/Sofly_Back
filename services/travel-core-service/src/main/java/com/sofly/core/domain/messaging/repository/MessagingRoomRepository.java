package com.sofly.core.domain.messaging.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofly.core.domain.messaging.entity.MessagingRoom;
import com.sofly.core.domain.messaging.enums.ChatRoomType;

public interface MessagingRoomRepository extends JpaRepository<MessagingRoom, Long> {
    Optional<MessagingRoom> findByTypeAndWorkspaceId(ChatRoomType type, Long workspaceId);
}

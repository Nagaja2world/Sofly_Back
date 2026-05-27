package com.sofly.core.domain.chat.repository;

import com.sofly.core.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByWorkspaceIdOrderByIdDesc(Long workspaceId);
}

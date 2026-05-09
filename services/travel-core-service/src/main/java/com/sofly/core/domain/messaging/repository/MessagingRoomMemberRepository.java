package com.sofly.core.domain.messaging.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sofly.core.domain.messaging.entity.MessagingRoomMember;

public interface MessagingRoomMemberRepository extends JpaRepository<MessagingRoomMember, Long> {

    boolean existsByMessagingRoomIdAndUserId(Long messagingRoomId, Long userId);

    @Query("SELECT mrm.messagingRoom.id FROM MessagingRoomMember mrm WHERE mrm.userId = :userId")
    List<Long> findRoomIdsByUserId(@Param("userId") Long userId);

}

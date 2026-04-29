package com.sofly.core.domain.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofly.core.domain.messaging.entity.MessagingRoomMember;

public interface MessagingRoomMemberRepository extends JpaRepository<MessagingRoomMember, Long> {
}

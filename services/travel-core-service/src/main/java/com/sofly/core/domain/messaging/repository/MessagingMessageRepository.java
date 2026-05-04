package com.sofly.core.domain.messaging.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.sofly.core.domain.messaging.document.MessagingMessage;

public interface MessagingMessageRepository extends MongoRepository<MessagingMessage, String> {
    Page<MessagingMessage> findByMessagingRoomIdOrderByCreatedAtDesc(Long messagingRoomId, Pageable pageable);
}

package com.sofly.core.domain.messaging.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sofly.core.domain.messaging.document.MessagingMessage;

public interface MessagingMessageRepository extends MongoRepository<MessagingMessage, String> {
}

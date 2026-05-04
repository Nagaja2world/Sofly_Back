package com.sofly.core.domain.messaging.document;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.sofly.core.domain.messaging.enums.ChatMessageType;

import lombok.*;

@Document(collection = "messaging_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MessagingMessage {

    @Id
    private String id;

    @Indexed
    private Long messagingRoomId;

    private Long senderId;

    private String senderNickname;

    private String content;

    private ChatMessageType type;

    private LocalDateTime createdAt;
}

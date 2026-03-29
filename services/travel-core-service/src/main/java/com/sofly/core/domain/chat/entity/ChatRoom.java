package com.sofly.core.domain.chat.entity;

import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workspaceId;

    @Column(nullable = false)
    private String title;        // 슬라이드에 보여줄 제목

    private String lastMessage;  // 마지막 메시지 미리보기

    public void updateLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void updateTitle(String title) {
        this.title = title;
    }
}

package com.sofly.core.domain.messaging.entity;

import java.time.LocalDateTime;

import com.sofly.core.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "messaging_room_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MessagingRoomMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "messaging_room_id", nullable = false)
    private MessagingRoom messagingRoom;

    @Column(nullable = false)
    private Long userId;

    private LocalDateTime lastReadAt;
}

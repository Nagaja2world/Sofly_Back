# FR-030 채팅 도메인 인프라 세팅

## 브랜치
```
feat/FR-030-chat-domain-infra-setup
```

---

## 작업 개요

실시간 채팅 기능 구현을 위한 도메인 설계 및 인프라 환경 세팅.
기존 AI 채팅(`chat` 패키지)과의 충돌을 피하기 위해 실시간 채팅은 `messaging` 패키지로 분리.

### 기술 스택
- **PostgreSQL** — 채팅방 메타 데이터 (`MessagingRoom`, `MessagingRoomMember`)
- **MongoDB Atlas** — 메시지 저장 (`MessagingMessage`)
- **Redis Pub/Sub** — 실시간 브로드캐스트
- **WebSocket + STOMP** — 실시간 통신

---

## 1. 의존성 추가 (`build.gradle`)

WebSocket은 기존에 이미 추가되어 있었음. MongoDB만 신규 추가.

```gradle
// MongoDB
implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
```

---

## 2. 환경변수 설정 (`.env`)

```env
MONGODB_URI=mongodb+srv://sofly_db_user:{password}@sofly.ftzblki.mongodb.net/?appName=Sofly
MONGODB_DATABASE=sofly_chat
```

---

## 3. `application.yml` 설정 추가

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
    mongodb:
      uri: ${MONGODB_URI}
      database: ${MONGODB_DATABASE:sofly_chat}
```

> WebSocket은 yml 설정 없이 `@Configuration` 클래스로만 처리.

---

## 4. MongoDB Atlas 네트워크 설정

- **Security → Network Access → IP Access List**
- 개발 환경: `0.0.0.0/0` 추가 (Allow Access from Anywhere)
- 배포 전 서버 고정 IP로 교체 필요

---

## 5. 패키지 구조

```
com.sofly.core.domain.messaging
├── enums/
│   ├── ChatRoomType.java
│   └── ChatMessageType.java
├── entity/
│   ├── MessagingRoom.java
│   └── MessagingRoomMember.java
├── document/
│   └── MessagingMessage.java
└── repository/
    ├── MessagingRoomRepository.java
    ├── MessagingRoomMemberRepository.java
    └── MessagingMessageRepository.java
```

> 기존 `chat` 패키지 (AI 채팅)는 수정 없이 유지.
> 테이블명 충돌 방지: `messaging_rooms`, `messaging_room_members` 사용.

---

## 6. Enum

### `ChatRoomType`
```java
package com.sofly.core.domain.messaging.enums;

public enum ChatRoomType {
    DIRECT,     // 1:1 채팅
    GROUP,      // 초대 기반 그룹 채팅
    WORKSPACE   // 워크스페이스 연동 채팅
}
```

### `ChatMessageType`
```java
package com.sofly.core.domain.messaging.enums;

public enum ChatMessageType {
    TEXT,
    IMAGE,
    FILE
}
```

---

## 7. PostgreSQL Entity

### `MessagingRoom`
```java
package com.sofly.core.domain.messaging.entity;

import com.sofly.core.domain.messaging.enums.ChatRoomType;
import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "messaging_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MessagingRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    private String name;

    private Long workspaceId;
}
```

### `MessagingRoomMember`
```java
package com.sofly.core.domain.messaging.entity;

import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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
```

---

## 8. MongoDB Document

### `MessagingMessage`
```java
package com.sofly.core.domain.messaging.document;

import com.sofly.core.domain.messaging.enums.ChatMessageType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

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
```

---

## 9. Repository

```java
// JPA
public interface MessagingRoomRepository extends JpaRepository<MessagingRoom, Long> {}

public interface MessagingRoomMemberRepository extends JpaRepository<MessagingRoomMember, Long> {}

// MongoDB
public interface MessagingMessageRepository extends MongoRepository<MessagingMessage, String> {}
```

---

## 10. 주요 Import 정리

| 용도 | Import |
|------|--------|
| JPA Entity 관련 | `jakarta.persistence.*` |
| MongoDB Document | `org.springframework.data.mongodb.core.mapping.Document` |
| MongoDB Id | `org.springframework.data.annotation.Id` |
| MongoDB Indexed | `org.springframework.data.mongodb.core.index.Indexed` (**Redis꺼 아님 주의**) |
| JPA Id | `jakarta.persistence.Id` |

---

## 11. 동작 확인

`bootRun` 실행 시 아래 로그 확인:

```log
// MongoDB Atlas 연결 성공
Monitor thread successfully connected to server...
  ac-9t91qkd-shard-00-00 → REPLICA_SET_SECONDARY ✅
  ac-9t91qkd-shard-00-01 → REPLICA_SET_SECONDARY ✅
  ac-9t91qkd-shard-00-02 → REPLICA_SET_PRIMARY   ✅
Discovered replica set primary... ✅
```

---

## 참고 사항

- Redis 컨테이너: `sofly-redis` (0.0.0.0:6379)
- MongoDB URI는 `.env`로 관리, `.gitignore`에 `.env` 추가 필수
- `RedisConfig`, `WebSocketConfig` 설정 클래스는 다음 단계에서 작성

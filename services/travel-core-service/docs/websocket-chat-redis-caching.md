# 이슈: 채팅 메시지 전송 시 PostgreSQL 중복 조회 성능 개선

## 이슈 배경

현재 채팅 메시지가 전송될 때마다 PostgreSQL을 2회 조회하고 있어요.
채팅 서비스 특성상 메시지가 초당 수십~수백 건 발생할 수 있어서 성능 병목이 될 수 있어요.

```
현재 메시지 1건 전송 시:
1. messagingRoomRepository.findById()          ← PostgreSQL 조회
2. messagingRoomMemberRepository.existsBy...() ← PostgreSQL 조회
3. messagingMessageRepository.save()           ← MongoDB 저장
4. redisTemplate.convertAndSend()              ← Redis publish
```

100명이 동시에 채팅하면 초당 수백 번 PostgreSQL을 조회하게 돼요.

---

## 개선 목표

메시지 전송 시 PostgreSQL 조회를 제거하고 Redis 캐시로 대체해요.

```
개선 후 메시지 1건 전송 시:
1. Redis에서 멤버십 확인 (캐시 HIT)  ← PostgreSQL 조회 없음
2. messagingMessageRepository.save()  ← MongoDB 저장
3. redisTemplate.convertAndSend()     ← Redis publish
```

---

## 현재 프로젝트 구조

```
com.sofly.core
├── domain/messaging
│   ├── controller/
│   │   └── MessagingController.java       ← 수정 필요
│   ├── entity/
│   │   ├── MessagingRoom.java
│   │   └── MessagingRoomMember.java
│   ├── document/
│   │   └── MessagingMessage.java
│   ├── repository/
│   │   ├── MessagingRoomRepository.java
│   │   ├── MessagingRoomMemberRepository.java  ← 수정 필요
│   │   └── MessagingMessageRepository.java
│   ├── service/
│   │   └── MessagingService.java          ← 수정 필요
│   └── dto/
│       ├── MessagingMessageRequest.java
│       ├── MessagingMessageResponse.java
│       └── MessagingRoomCreateRequest.java
└── global
    ├── config/
    │   ├── RedisConfig.java
    │   ├── WebSocketConfig.java
    │   └── StompChannelInterceptor.java    ← 수정 필요
    └── redis/
        └── RedisSubscriber.java
```

---

## 수정 파일 및 작업 상세

### 1. `MessagingRoomMemberRepository.java` — 쿼리 메서드 추가

**파일 위치**: `com.sofly.core.domain.messaging.repository.MessagingRoomMemberRepository`

**현재 코드**:
```java
public interface MessagingRoomMemberRepository extends JpaRepository<MessagingRoomMember, Long> {
    boolean existsByMessagingRoomIdAndUserId(Long messagingRoomId, Long userId);
}
```

**추가할 코드**:
```java
public interface MessagingRoomMemberRepository extends JpaRepository<MessagingRoomMember, Long> {
    boolean existsByMessagingRoomIdAndUserId(Long messagingRoomId, Long userId);

    // ✅ 유저가 속한 채팅방 ID 목록 조회 (캐싱용)
    @Query("SELECT m.messagingRoom.id FROM MessagingRoomMember m WHERE m.userId = :userId")
    List<Long> findRoomIdsByUserId(@Param("userId") Long userId);
}
```

import 추가:
```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
```

---

### 2. `StompChannelInterceptor.java` — 연결 시점에 멤버십 캐싱

**파일 위치**: `com.sofly.core.global.config.StompChannelInterceptor`

**현재 코드**:
```java
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())
                || StompCommand.SEND.equals(accessor.getCommand())
                || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        Long userId = jwtTokenProvider.getUserId(token);
                        Authentication auth = new UsernamePasswordAuthenticationToken(
                                userId, null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                        accessor.setUser(auth);
                        accessor.getSessionAttributes().put("nickname", /* DB 조회 */);
                    }
                } catch (Exception e) {
                    log.warn("WebSocket JWT 인증 실패: {}", e.getMessage());
                }
            }
        }
        return message;
    }
}
```

**수정할 코드**:
```java
@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;                           // ← 기존
    private final MessagingRoomMemberRepository messagingRoomMemberRepository; // ← 추가
    private final RedisTemplate<String, Object> redisTemplate;             // ← 추가

    // Redis 캐시 키 패턴: "user:rooms:{userId}"
    private static final String USER_ROOMS_CACHE_KEY = "user:rooms:";
    // 캐시 만료 시간: 1시간
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())
                || StompCommand.SEND.equals(accessor.getCommand())
                || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        Long userId = jwtTokenProvider.getUserId(token);

                        // ✅ CONNECT 시점에만 DB 조회 + 캐싱
                        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                            // 유저 닉네임 조회 (1회)
                            User user = userRepository.findById(userId)
                                    .orElseThrow(() -> new RuntimeException("User not found"));

                            // 유저가 속한 채팅방 ID 목록 조회 (1회) → Redis 캐싱
                            List<Long> roomIds = messagingRoomMemberRepository
                                    .findRoomIdsByUserId(userId);

                            String cacheKey = USER_ROOMS_CACHE_KEY + userId;
                            if (!roomIds.isEmpty()) {
                                redisTemplate.opsForSet().add(cacheKey,
                                        roomIds.stream().map(Object.class::cast).toArray());
                                redisTemplate.expire(cacheKey, CACHE_TTL);
                            }

                            // 닉네임 세션에 저장
                            accessor.getSessionAttributes().put("nickname", user.getNickname());
                            accessor.getSessionAttributes().put("userId", userId);
                        }

                        Authentication auth = new UsernamePasswordAuthenticationToken(
                                userId, null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                        accessor.setUser(auth);
                    }
                } catch (Exception e) {
                    log.warn("WebSocket JWT 인증 실패: {}", e.getMessage());
                }
            }
        }
        return message;
    }
}
```

import 추가:
```java
import com.sofly.core.domain.messaging.repository.MessagingRoomMemberRepository;
import com.sofly.core.domain.user.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;
import java.util.List;
```

---

### 3. `MessagingService.java` — Redis 캐시로 멤버십 확인

**파일 위치**: `com.sofly.core.domain.messaging.service.MessagingService`

**현재 코드**:
```java
public MessagingMessageResponse sendMessage(
        Long roomId,
        MessagingMessageRequest request,
        Long senderId,
        String senderNickname) {

    // 1. 채팅방 존재 확인 ← PostgreSQL 조회
    messagingRoomRepository.findById(roomId)
            .orElseThrow(() -> new SoflyException(ErrorCode.ROOM_NOT_FOUND));

    // 2. 멤버 여부 확인 ← PostgreSQL 조회
    boolean isMember = messagingRoomMemberRepository
            .existsByMessagingRoomIdAndUserId(roomId, senderId);
    if (!isMember) {
        throw new SoflyException(ErrorCode.MESSAGING_ROOM_ACCESS_DENIED);
    }
    ...
}
```

**수정할 코드**:
```java
private static final String USER_ROOMS_CACHE_KEY = "user:rooms:";

public MessagingMessageResponse sendMessage(
        Long roomId,
        MessagingMessageRequest request,
        Long senderId,
        String senderNickname) {

    // ✅ Redis 캐시에서 멤버십 확인 (PostgreSQL 조회 없음)
    String cacheKey = USER_ROOMS_CACHE_KEY + senderId;
    Boolean isMember = redisTemplate.opsForSet().isMember(cacheKey, roomId);

    if (!Boolean.TRUE.equals(isMember)) {
        // 캐시 MISS 시 DB fallback + 재캐싱
        boolean isMemberInDb = messagingRoomMemberRepository
                .existsByMessagingRoomIdAndUserId(roomId, senderId);
        if (!isMemberInDb) {
            throw new SoflyException(ErrorCode.MESSAGING_ROOM_ACCESS_DENIED);
        }
        // 재캐싱
        redisTemplate.opsForSet().add(cacheKey, roomId);
        redisTemplate.expire(cacheKey, Duration.ofHours(1));
    }

    // 채팅방 존재 확인은 캐시 HIT이면 생략 가능
    // (멤버로 등록됐다는 건 채팅방이 존재한다는 의미)

    MessagingMessage message = MessagingMessage.builder()
            .messagingRoomId(roomId)
            .senderId(senderId)
            .senderNickname(senderNickname)
            .content(request.content())
            .type(request.type())
            .createdAt(LocalDateTime.now())
            .build();

    MessagingMessage saved = messagingMessageRepository.save(message);
    redisTemplate.convertAndSend(channelTopic.getTopic(), saved);
    return MessagingMessageResponse.from(saved);
}
```

---

### 4. `MessagingService.java` — 채팅방 생성 시 캐시 업데이트

채팅방이 새로 생성될 때 해당 멤버들의 Redis 캐시도 업데이트해야 해요.

**`createRoom()` 메서드에 캐시 업데이트 추가**:
```java
@Transactional
public MessagingRoom createRoom(MessagingRoomCreateRequest request) {
    MessagingRoom room = MessagingRoom.builder()
            .type(request.type())
            .name(request.name())
            .workspaceId(request.workspaceId())
            .build();

    MessagingRoom savedRoom = messagingRoomRepository.save(room);

    List<MessagingRoomMember> members = request.memberIds().stream()
            .map(userId -> MessagingRoomMember.builder()
                    .messagingRoom(savedRoom)
                    .userId(userId)
                    .build())
            .toList();

    messagingRoomMemberRepository.saveAll(members);

    // ✅ 각 멤버의 Redis 캐시에 새 채팅방 추가
    request.memberIds().forEach(userId -> {
        String cacheKey = USER_ROOMS_CACHE_KEY + userId;
        // 캐시가 존재하면 업데이트, 없으면 무시 (다음 CONNECT 시 갱신됨)
        Boolean hasCache = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(hasCache)) {
            redisTemplate.opsForSet().add(cacheKey, savedRoom.getId());
        }
    });

    return savedRoom;
}
```

---

### 5. `MessagingController.java` — senderNickname 세션에서 꺼내기

**현재 코드**:
```java
@MessageMapping("/chat.message.{roomId}")
public void sendMessage(
        @DestinationVariable Long roomId,
        MessagingMessageRequest request,
        Principal principal) {

    Long senderId = Long.parseLong(principal.getName());
    messagingService.sendMessage(roomId, request, senderId);
}
```

**수정할 코드**:
```java
@MessageMapping("/chat.message.{roomId}")
public void sendMessage(
        @DestinationVariable Long roomId,
        MessagingMessageRequest request,
        Principal principal,
        SimpMessageHeaderAccessor headerAccessor) {  // ← 추가

    Long senderId = Long.parseLong(principal.getName());

    // ✅ 세션에서 닉네임 꺼냄 (DB 조회 없음)
    String senderNickname = (String) headerAccessor.getSessionAttributes().get("nickname");

    messagingService.sendMessage(roomId, request, senderId, senderNickname);
}
```

import 추가:
```java
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
```

---

## 캐시 무효화 전략

캐시가 오래되면 잘못된 멤버십 정보를 가질 수 있어요. 아래 상황에서 캐시를 무효화해야 해요.

| 상황 | 캐시 처리 |
|------|-----------|
| 채팅방 생성 | 해당 멤버 캐시에 roomId 추가 |
| 멤버 강퇴/탈퇴 | 해당 유저 캐시에서 roomId 제거 |
| WebSocket 연결 | 전체 캐시 갱신 |
| TTL 만료 (1시간) | 자동 만료 후 재연결 시 갱신 |

멤버 강퇴/탈퇴 기능 구현 시 아래 코드 추가:
```java
// 강퇴/탈퇴 시 캐시에서 제거
redisTemplate.opsForSet().remove("user:rooms:" + userId, roomId);
```

---

## 개선 전/후 성능 비교

```
❌ 개선 전 (메시지 1건당)
PostgreSQL 2회 조회 + MongoDB 1회 저장 + Redis 1회 publish

✅ 개선 후 (메시지 1건당)
Redis 1회 조회(캐시 HIT) + MongoDB 1회 저장 + Redis 1회 publish

PostgreSQL 조회 완전 제거!
캐시 MISS 시에만 PostgreSQL 1회 조회 + 재캐싱
```

---

## 주의 사항

- `redisTemplate.opsForSet().isMember(key, value)` 에서 value 타입이 `Long`이어야 해요. Redis에 저장할 때와 조회할 때 타입이 일치해야 캐시 HIT이 발생해요.
- `GenericJackson2JsonRedisSerializer` 사용 시 `Long` 타입이 `Integer`로 역직렬화될 수 있어요. 이 경우 타입 캐스팅 주의가 필요해요.
- TTL은 1시간으로 설정했지만, 서비스 특성에 맞게 조정 가능해요.

---

## 관련 파일 요약

| 파일 | 작업 내용 |
|------|-----------|
| `MessagingRoomMemberRepository.java` | `findRoomIdsByUserId()` 쿼리 메서드 추가 |
| `StompChannelInterceptor.java` | CONNECT 시점에 멤버십 Redis 캐싱 |
| `MessagingService.java` | Redis 캐시로 멤버십 확인, DB fallback 처리, createRoom 시 캐시 업데이트 |
| `MessagingController.java` | `SimpMessageHeaderAccessor`로 세션에서 닉네임 꺼내기 |

## 관련 이슈
- 상위 이슈: FR-030 실시간 채팅 도메인 설계 및 인프라 세팅

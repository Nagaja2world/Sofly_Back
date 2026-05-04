# FR-030 실시간 채팅 도메인 — 개념 및 원리 정리

## 목차
1. [전체 아키텍처](#1-전체-아키텍처)
2. [기술 스택 선택 이유](#2-기술-스택-선택-이유)
3. [WebSocket + STOMP](#3-websocket--stomp)
4. [Redis Pub/Sub](#4-redis-pubsub)
5. [MongoDB 메시지 저장](#5-mongodb-메시지-저장)
6. [JWT 인증 흐름](#6-jwt-인증-흐름)
7. [패키지 구조 및 설계 결정](#7-패키지-구조-및-설계-결정)
8. [데이터 흐름 전체 시나리오](#8-데이터-흐름-전체-시나리오)
9. [Nginx WebSocket 설정](#9-nginx-websocket-설정)
10. [트러블슈팅 기록](#10-트러블슈팅-기록)

---

## 1. 전체 아키텍처

```
클라이언트 A                      클라이언트 B
    │                                  │
    │ WebSocket (STOMP)                │ WebSocket (STOMP)
    ▼                                  ▼
┌─────────────────────────────────────────────┐
│              Spring Boot Server              │
│                                             │
│  ┌──────────────┐    ┌───────────────────┐  │
│  │ STOMP Broker │    │ MessagingService  │  │
│  │  /sub/**     │    │  - 메시지 저장     │  │
│  └──────┬───────┘    └────────┬──────────┘  │
│         │                     │             │
│         │              ┌──────▼──────┐      │
│         │              │   MongoDB   │      │
│         │              │  (메시지)   │      │
│         │              └─────────────┘      │
│         │                                   │
│  ┌──────▼───────────────────────────────┐   │
│  │         Redis Pub/Sub                │   │
│  │  publish → "messaging" 채널          │   │
│  │  subscribe → RedisSubscriber         │   │
│  └──────────────────────────────────────┘   │
│         │                                   │
│  ┌──────▼───────┐                           │
│  │SimpMessaging │                           │
│  │  Template    │ → /sub/chat/{roomId}      │
│  └─────────────┘                            │
└─────────────────────────────────────────────┘
         │                        │
    클라이언트 A               클라이언트 B
    메시지 수신                메시지 수신
```

---

## 2. 기술 스택 선택 이유

### PostgreSQL — 채팅방 메타데이터
채팅방(`MessagingRoom`)과 멤버(`MessagingRoomMember`)는 **관계형 데이터**예요.
- 채팅방 ID, 타입, 워크스페이스 연관, 멤버 목록 등 구조가 명확하고 JOIN 쿼리가 필요해요.
- 트랜잭션 보장이 필요해요.

### MongoDB — 메시지 저장
채팅 메시지는 **비정형 대용량 데이터**예요.
- 메시지 수가 폭발적으로 증가해요.
- 스키마가 유연해야 해요 (TEXT, IMAGE, FILE 타입마다 다른 필드).
- 읽기 성능이 중요해요 (채팅방별 최신 메시지 조회).
- `chatRoomId` 기준 인덱스(`@Indexed`)로 빠른 조회 가능해요.

### Redis Pub/Sub — 실시간 브로드캐스트
실시간 메시지 전파에 Redis를 쓰는 이유:
- **서버 확장성**: 서버가 여러 대로 늘어나도 Redis를 통해 모든 서버에 메시지 전파 가능해요.
- 단순 WebSocket만 쓰면 같은 서버에 연결된 클라이언트끼리만 통신 가능해요.
- 메시지를 Redis 채널에 publish하면 모든 서버의 subscriber가 받아서 자기 클라이언트에게 전달해요.

### WebSocket + STOMP — 실시간 통신
- **WebSocket**: HTTP 핸드셰이크 후 지속 연결 유지. 양방향 통신.
- **STOMP**: WebSocket 위의 메시징 프로토콜. 발행/구독 패턴 지원.
  - 순수 WebSocket은 메시지 라우팅이 없어요.
  - STOMP는 `/pub`, `/sub` prefix로 목적지 라우팅 제공해요.

---

## 3. WebSocket + STOMP

### WebSocket이란?
HTTP는 클라이언트가 요청해야만 서버가 응답해요 (단방향). WebSocket은 한 번 연결하면 서버도 클라이언트에게 먼저 데이터를 보낼 수 있어요 (양방향).

```
HTTP:    클라이언트 → 요청 → 서버 → 응답 → 끝
WebSocket: 클라이언트 ↔ 서버 (연결 유지, 양방향)
```

### STOMP(Simple Text Oriented Messaging Protocol)
WebSocket은 raw 바이트를 주고받는데, 어디로 보낼지 라우팅 정보가 없어요. STOMP가 이를 해결해요.

```
STOMP 프레임 구조:
COMMAND
header1:value1
header2:value2

Body
^@
```

주요 커맨드:
- `CONNECT`: 서버에 연결
- `SUBSCRIBE`: 특정 destination 구독
- `SEND`: 메시지 발행
- `DISCONNECT`: 연결 해제

### 설정

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");        // 구독 prefix
        registry.setApplicationDestinationPrefixes("/pub"); // 발행 prefix
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // WebSocket 미지원 브라우저 fallback
    }
}
```

### SockJS란?
일부 환경(방화벽, 구형 브라우저)에서 WebSocket이 차단될 수 있어요. SockJS는 WebSocket 연결을 먼저 시도하고, 실패하면 Long Polling 등 대안 방식으로 자동 전환해요.

### 클라이언트 연결 방법 (JavaScript)
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const client = Stomp.over(socket);

// 연결 (JWT 헤더 포함)
client.connect({ Authorization: 'Bearer ' + token }, () => {
    // 구독
    client.subscribe('/sub/chat/1', (message) => {
        console.log(JSON.parse(message.body));
    });
});

// 메시지 발행
client.send('/pub/chat.message.1', {}, JSON.stringify({
    content: '안녕하세요',
    type: 'TEXT'
}));
```

---

## 4. Redis Pub/Sub

### 원리

```
서버 A                          서버 B
  │                               │
  │ publish("messaging", msg)     │
  ▼                               │
Redis ──────────────────────────► │
  │                          onMessage() 수신
  │                               │
  │                          SimpMessagingTemplate
  │                               │
  │                          /sub/chat/{roomId}로 전파
```

### 왜 필요한가?
서버가 1대일 때: WebSocket 세션이 모두 같은 서버에 있으므로 직접 브로드캐스트 가능.
서버가 N대일 때: 클라이언트 A는 서버1, 클라이언트 B는 서버2에 연결된 경우, 서버1이 직접 B에게 보낼 수 없어요. Redis를 통해 서버2에 전달해요.

### 구성 요소

**RedisConfig — 채널 및 리스너 등록**
```java
@Bean
public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        MessageListenerAdapter messageListenerAdapter) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(messageListenerAdapter, channelTopic()); // 채널 구독
    return container;
}

@Bean
public ChannelTopic channelTopic() {
    return new ChannelTopic("messaging"); // Redis 채널명
}
```

**RedisSubscriber — 수신 후 WebSocket 브로드캐스트**
```java
@Component
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        MessagingMessage msg = objectMapper.readValue(message.getBody(), MessagingMessage.class);
        // WebSocket 구독자에게 전달
        messagingTemplate.convertAndSend("/sub/chat/" + msg.getMessagingRoomId(), msg);
    }
}
```

### LocalDateTime 직렬화 문제
Redis의 `GenericJackson2JsonRedisSerializer`는 기본적으로 Java 8 날짜 타입(`LocalDateTime`)을 모르기 때문에 `JavaTimeModule`을 등록해야 해요.

```java
@Bean
public GenericJackson2JsonRedisSerializer redisSerializer() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return new GenericJackson2JsonRedisSerializer(objectMapper);
}
```

---

## 5. MongoDB 메시지 저장

### Document 설계

```java
@Document(collection = "messaging_messages")
public class MessagingMessage {

    @Id
    private String id;          // MongoDB ObjectId (String)

    @Indexed
    private Long messagingRoomId; // 조회 성능을 위한 인덱스

    private Long senderId;
    private String senderNickname;
    private String content;
    private ChatMessageType type; // TEXT, IMAGE, FILE
    private LocalDateTime createdAt;
}
```

### @Indexed의 역할
`messagingRoomId`로 메시지를 조회할 때 인덱스가 없으면 전체 컬렉션 스캔(Full Scan)이 발생해요. `@Indexed`를 붙이면 MongoDB가 해당 필드에 인덱스를 생성해서 조회 속도가 O(n) → O(log n)으로 빨라져요.

### Spring Data MongoDB vs JPA
| | JPA (PostgreSQL) | Spring Data MongoDB |
|--|--|--|
| 어노테이션 | `@Entity` | `@Document` |
| ID | `jakarta.persistence.Id` | `org.springframework.data.annotation.Id` |
| Repository | `JpaRepository` | `MongoRepository` |
| 쿼리 | JPQL / SQL | MongoDB Query |

### 페이징 처리
```java
// Repository
Page<MessagingMessage> findByMessagingRoomIdOrderByCreatedAtDesc(
        Long messagingRoomId, Pageable pageable);

// 호출
PageRequest.of(0, 30) // 0번 페이지, 30개씩
```

최신 메시지부터(`Desc`) 가져와야 페이징이 자연스러워요. 클라이언트에서 역순으로 렌더링하면 돼요.

---

## 6. JWT 인증 흐름

### HTTP vs WebSocket 인증 차이

HTTP 요청은 `JwtAuthenticationFilter`가 매 요청마다 JWT를 파싱해서 `SecurityContextHolder`에 넣어줘요. 하지만 WebSocket은 최초 연결(핸드셰이크) 이후 지속 연결이라 HTTP 필터를 거치지 않아요.

```
HTTP 요청:
클라이언트 → JwtAuthenticationFilter → SecurityContextHolder 세팅 → Controller

WebSocket STOMP:
클라이언트 → 핸드셰이크 (1회) → 이후 STOMP 프레임 교환
              ↑
        HTTP 필터 여기서만 동작
        이후 STOMP 메시지는 필터 미적용!
```

### 해결: StompChannelInterceptor

```java
@Component
public class StompChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, ...);

        if (StompCommand.CONNECT.equals(accessor.getCommand())
                || StompCommand.SEND.equals(accessor.getCommand())) {

            String token = accessor.getFirstNativeHeader("Authorization").substring(7);

            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserId(token);
                Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, ...);
                accessor.setUser(auth); // WebSocket 세션에 유저 바인딩
            }
        }
        return message;
    }
}
```

### SecurityContextHolder vs accessor.setUser()
처음에 `SecurityContextHolder.getContext().setAuthentication(auth)`를 썼더니 동작하지 않았어요.

이유: `SecurityContextHolder`는 **스레드 로컬** 기반이에요. 인터셉터가 실행되는 스레드와 `@MessageMapping` 핸들러가 실행되는 스레드가 달라서 SecurityContext가 공유되지 않아요.

해결: `accessor.setUser(auth)`로 WebSocket 세션 자체에 유저 정보를 바인딩하고, 컨트롤러에서 `Principal`로 받아요.

```java
// 인터셉터
accessor.setUser(auth);  // WebSocket 세션에 바인딩

// 컨트롤러
@MessageMapping("/chat.message.{roomId}")
public MessagingMessageResponse sendMessage(
        @DestinationVariable Long roomId,
        MessagingMessageRequest request,
        Principal principal) {  // 세션에서 꺼내옴

    Long senderId = Long.parseLong(principal.getName());
}
```

---

## 7. 패키지 구조 및 설계 결정

### 기존 `chat` 패키지와의 충돌

기존 `chat` 패키지는 AI 채팅(Gemini) 전용이었고, 실시간 채팅을 같은 이름으로 만들면 클래스명 충돌이 발생해요.

```
com.sofly.core.domain.chat      ← AI 채팅 (기존, 유지)
com.sofly.core.domain.messaging ← 실시간 채팅 (신규)
```

클래스명도 `Messaging` prefix로 통일:
- `MessagingRoom` (테이블: `messaging_rooms`)
- `MessagingRoomMember` (테이블: `messaging_room_members`)
- `MessagingMessage` (컬렉션: `messaging_messages`)

### 채팅방 타입 설계

```java
public enum ChatRoomType {
    DIRECT,     // 1:1 채팅
    GROUP,      // 초대 기반 그룹 채팅
    WORKSPACE   // 워크스페이스 연동 채팅
}
```

### 다중 Spring Data 모듈 설정
JPA, MongoDB, Redis가 함께 있으면 Spring이 Repository를 어느 DB로 연결할지 혼란스러워해요. 각 Repository가 올바른 어노테이션(`@Entity` vs `@Document`)을 가져야 Spring Data가 strict 모드에서 정확히 분류해요.

```log
// 정상 동작 로그
Found 15 JPA repository interfaces.
Found 1 MongoDB repository interface.
```

---

## 8. 데이터 흐름 전체 시나리오

### 메시지 전송 흐름

```
1. 클라이언트가 STOMP SEND 프레임 전송
   destination: /pub/chat.message.{roomId}
   header: Authorization: Bearer {jwt}
   body: { "content": "안녕", "type": "TEXT" }

2. StompChannelInterceptor.preSend()
   → JWT 파싱 → userId 추출
   → accessor.setUser(auth)

3. MessagingController.sendMessage()
   → Principal에서 userId 추출
   → MessagingService.sendMessage() 호출

4. MessagingService
   → UserRepository로 nickname 조회
   → MessagingMessage 생성
   → MongoDB에 저장
   → Redis "messaging" 채널에 publish

5. RedisSubscriber.onMessage()
   → Redis 메시지 수신
   → SimpMessagingTemplate으로 브로드캐스트
   → /sub/chat/{roomId} 구독자 전체에게 전달

6. 클라이언트들 메시지 수신
```

### 채팅방 생성 흐름

```
POST /api/v1/messaging/rooms
→ MessagingRoom 생성 (PostgreSQL)
→ MessagingRoomMember 등록 (PostgreSQL)
```

### 메시지 히스토리 조회 흐름

```
GET /api/v1/messaging/rooms/{roomId}/messages?page=0&size=30
→ MongoDB에서 최신순 조회 (Desc)
→ Page<MessagingMessageResponse> 반환
```

---

## 9. Nginx WebSocket 설정

### 왜 별도 설정이 필요한가?
WebSocket은 HTTP 1.1의 `Upgrade` 메커니즘을 사용해요. 클라이언트가 `Connection: Upgrade`, `Upgrade: websocket` 헤더를 보내면 서버가 프로토콜을 전환해요. Nginx가 기본적으로 이 헤더를 전달하지 않아서 별도 설정이 필요해요.

```nginx
location /ws/ {
    proxy_pass $core_url;
    proxy_http_version 1.1;              # WebSocket은 HTTP/1.1 필수
    proxy_set_header Upgrade $http_upgrade; # 업그레이드 헤더 전달
    proxy_set_header Connection "upgrade";  # 연결 유지
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 3600s;            # 1시간 (기본 60초면 채팅 중 끊김)
    proxy_send_timeout 3600s;
}
```

### 타임아웃 설정이 중요한 이유
Nginx 기본 `proxy_read_timeout`은 60초예요. 채팅방을 열어놓고 60초간 메시지가 없으면 Nginx가 연결을 끊어버려요. 채팅 서비스 특성상 유저가 장시간 대기하는 경우가 많아서 충분히 늘려야 해요.

---

## 10. 트러블슈팅 기록

### 1. MongoDB Repository 인식 실패 (Found 0 MongoDB repository interfaces)
**원인**: `ChatMessageRepository`가 `MongoRepository`를 extends하지 않았거나 `@Document` 어노테이션 누락.

**해결**: `MongoRepository<MessagingMessage, String>` extends, `@Document` 추가.

---

### 2. LocalDateTime 직렬화 실패
```
SerializationException: Java 8 date/time type `java.time.LocalDateTime` not supported
```
**원인**: `GenericJackson2JsonRedisSerializer`의 기본 `ObjectMapper`가 Java 8 날짜 타입을 모름.

**해결**: `JavaTimeModule` 등록한 `ObjectMapper`로 커스텀 직렬화기 생성.
```java
objectMapper.registerModule(new JavaTimeModule());
objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

---

### 3. Redis 연결 실패
```
RedisConnectionFailureException: Unable to connect to Redis
```
**원인**: 로컬 Redis 컨테이너가 실행되지 않음.

**해결**: `docker-compose up sofly-redis`

---

### 4. WebSocket 401 Unauthorized
**원인**: `/ws/**` 경로가 Spring Security에서 차단됨.

**해결**: `SecurityConfig`에 `/ws/**` permitAll 추가.

---

### 5. 메시지 중복 표시 (본인만 2개)
**원인**: 메시지 전송 시 `addMessage()` 직접 호출 + 구독으로 수신 시 또 `addMessage()` 호출.

**해결**: 전송 시 직접 추가 코드 제거. 구독 수신으로만 표시.

---

### 6. 한글 IME 이중 전송
**원인**: `ㅅㄱ` 입력 후 Enter 시 조합 완성 이벤트 + Enter 이벤트 동시 발생.

**해결**: `e.isComposing` 체크 추가.
```javascript
function handleEnter(e) {
    if (e.isComposing) return; // 한글 조합 중이면 무시
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
}
```

---

### 7. AuthException — SecurityContext 스레드 문제
**원인**: `StompChannelInterceptor`에서 `SecurityContextHolder`에 세팅해도 `@MessageMapping` 핸들러는 다른 스레드에서 실행되어 SecurityContext가 비어있음.

**해결**: `accessor.setUser(auth)` + 컨트롤러에서 `Principal` 주입으로 변경.

---

## 참고 — 주요 어노테이션 정리

| 어노테이션 | 패키지 | 용도 |
|-----------|--------|------|
| `@Document` | `org.springframework.data.mongodb.core.mapping` | MongoDB 컬렉션 매핑 |
| `@Indexed` | `org.springframework.data.mongodb.core.index` | MongoDB 인덱스 생성 (Redis꺼 아님 주의!) |
| `@Id` (MongoDB) | `org.springframework.data.annotation` | MongoDB Document ID |
| `@Id` (JPA) | `jakarta.persistence` | JPA Entity ID |
| `@MessageMapping` | `org.springframework.messaging.handler.annotation` | STOMP 메시지 수신 |
| `@SendTo` | `org.springframework.messaging.handler.annotation` | STOMP 메시지 발행 destination |
| `@DestinationVariable` | `org.springframework.messaging.handler.annotation` | STOMP destination 경로 변수 |

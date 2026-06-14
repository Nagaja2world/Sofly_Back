# 백엔드 핵심 구현 기술 정리

> 작성일: 2026-06-14

---

## 1. Redis ZSet + Kafka 이벤트 기반 스케줄러 — 디스크 I/O 병목 제어

### 배경 및 문제 정의

항공편을 저장하면 해당 워크스페이스 멤버들의 '정복지도(Conquest Map)' 상태를 `PLANNED → VISITED`로 전환해야 한다.
이 전환 시점은 **출발 시각**이며, 매 요청마다 전체 DB를 폴링하면 불필요한 디스크 I/O가 반복된다.

### 해결 방안: Redis ZSet을 TTL-free 지연 큐로 활용

출발 시각을 score로 하는 ZSet에 멤버를 적재하고, 60초마다 실행되는 스케줄러가 `score ≤ 현재시각`인 항목만 꺼내 처리한다.
폴링 범위를 DB 전체가 아닌 ZSet 조건 조회로 한정하여 I/O를 최소화한다.

### 이벤트 흐름

```
[사용자: 항공편 저장 API]
        │
        ▼
WorkspaceService.saveFlight()
        │
        └── kafkaTemplate.send("flight.saved", FlightSavedMessage)
                │
                ▼
        ConquestConsumer.consume()
                │
                ├── 출발 시각이 이미 지남 → DB VISITED 즉시 전환
                │
                └── 출발 시각이 미래    → Redis ZSet 등록
                                              (score = 출발 epoch seconds)
                                                │
                                                ▼
                                    RedisFlightDepartureScheduler (60초 주기)
                                    → rangeByScore(0, now)
                                    → DB VISITED 전환 + ZSet 제거
```

### 핵심 코드

**Kafka Consumer — 항공편 이벤트 수신 및 Redis ZSet 적재**

```java
// ConquestConsumer.java
@KafkaListener(topics = "flight.saved", groupId = "travel-core-service",
        containerFactory = "kafkaListenerContainerFactory")
public void consume(FlightSavedMessage message) {
    AirportInfo arrivalInfo = airportInfoService.findByIata(message.getArrivalAirport()).orElse(null);
    if (arrivalInfo == null) {
        log.warn("알 수 없는 도착 공항 코드: {}", message.getArrivalAirport());
        return;
    }

    Instant now = Instant.now();
    Instant departureInstant = message.getDepartureTime().toInstant();

    for (Long userId : message.getMemberUserIds()) {
        try {
            User user = userRepository.getReferenceById(userId);
            conquestMapService.applyPlannedStatus(user, arrivalInfo); // PLANNED 마킹

            if (departureInstant.isBefore(now)) {
                // 출발이 이미 지났으면 DB에 즉시 VISITED 기록
                conquestMapService.promoteToVisited(userId, arrivalInfo.countryCode());
                log.info("즉시 VISITED 전환: userId={}, country={}", userId, arrivalInfo.countryCode());
            } else {
                // 미래 출발 → Redis ZSet에 등록 (score = 출발 epoch seconds)
                long epochSeconds = departureInstant.getEpochSecond();
                String member = userId + ":" + arrivalInfo.countryCode() + ":" + arrivalInfo.cityName();
                stringRedisTemplate.opsForZSet().add(FLIGHT_DEPARTURES_KEY, member, epochSeconds);
                log.info("Redis 등록: member={}, score={}", member, epochSeconds);
            }
        } catch (DataAccessException e) {
            // DB 오류 → ZSet에서 제거하지 않아 다음 Consumer 재시도 시 재처리
            log.error("ConquestConsumer DB 오류: userId={}, err={}", userId, e.getMessage(), e);
        }
    }
}
```

**스케줄러 — 주기적으로 만료된 ZSet 항목을 처리**

```java
// RedisFlightDepartureScheduler.java
private static final String KEY = "flight:departures";

@Scheduled(fixedDelay = 60_000)  // 60초마다 실행
public void processDeadlines() {
    double nowScore = Instant.now().getEpochSecond();

    // score가 현재 시각 이하인 항목만 조회 (DB 풀스캔 없음)
    Set<String> due = stringRedisTemplate.opsForZSet().rangeByScore(KEY, 0, nowScore);
    if (due == null || due.isEmpty()) return;

    log.info("출발 도래 항목 {}건 처리 시작", due.size());

    for (String member : due) {
        // member 포맷: {userId}:{countryCode}:{cityName}
        String[] parts = member.split(":", 3);
        try {
            Long userId = Long.parseLong(parts[0]);
            String countryCode = parts[1];

            conquestMapService.promoteToVisited(userId, countryCode); // DB VISITED 전환
            stringRedisTemplate.opsForZSet().remove(KEY, member);    // ZSet에서 제거
            log.info("PLANNED → VISITED 완료: userId={}, country={}", userId, countryCode);

        } catch (DataAccessException e) {
            // DB 오류 → ZSet에 남겨 두어 다음 60초 주기에 자동 재시도
            log.warn("DB 오류로 재시도 예정: member={}, err={}", member, e.getMessage());
        } catch (IllegalArgumentException e) {
            // 포맷 오류 → 재시도해도 의미 없으므로 즉시 제거
            log.error("잘못된 member 포맷, 제거: member={}, err={}", member, e.getMessage());
            stringRedisTemplate.opsForZSet().remove(KEY, member);
        }
    }
}
```

### Redis ZSet 키 구조

```
flight:departures  (ZSet)
  member : "{userId}:{countryCode}:{cityName}"   예) "42:JP:Tokyo"
  score  : 출발 시각 UTC epoch seconds            예) 1750000000
```

### 핵심 포인트 요약

| 항목 | 내용 |
|------|------|
| 병목 회피 전략 | DB 전체 폴링 대신 ZSet 조건 조회(`rangeByScore`)로 처리 대상만 추출 |
| 재시도 보장 | `DataAccessException` 발생 시 ZSet에서 제거하지 않아 60초 후 자동 재시도 |
| 포맷 오류 처리 | `IllegalArgumentException` 발생 시 ZSet에서 즉시 제거 (무한 재시도 방지) |
| 이벤트 분리 | Kafka로 항공편 저장 이벤트를 비동기 전파 → 정복지도 처리와 저장 로직 디커플링 |

---

## 2. 외부 API 과금 방어 — Redis 캐싱 TTL 전략

### 배경 및 문제 정의

`travel-supply-service`는 **Booking.com RapidAPI**(항공·호텔 검색)와 **Google Places API**(장소 검색·사진)를 호출한다.
외부 API는 호출 건수당 과금되므로, 동일한 조건의 반복 호출을 막는 캐싱 전략이 필수적이다.

### 해결 방안: 데이터 특성에 따른 TTL 차등 설정

변동성이 높은 실시간 검색 결과와 거의 바뀌지 않는 메타 데이터를 분리하여 TTL을 다르게 설정한다.

### 캐시 설정 (`RedisCacheConfig`)

```java
// RedisCacheConfig.java
@EnableCaching
@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // 항공/호텔 검색 결과: TTL 5분 (실시간 가격 반영, 단기 중복 요청 흡수)
        RedisCacheConfiguration searchConfig = jsonNodeCacheConfig(Duration.ofMinutes(5));

        // 목적지/정렬/필터 메타 데이터: TTL 24시간 (변경 빈도 낮음)
        RedisCacheConfiguration pojoMetaConfig = pojoCacheConfig(Duration.ofHours(24));

        // getFilter 메타 (JsonNode 반환): TTL 24시간
        RedisCacheConfiguration jsonNodeMetaConfig = jsonNodeCacheConfig(Duration.ofHours(24));

        // 장소 검색 결과: TTL 1시간
        RedisCacheConfiguration placeSearchConfig = pojoCacheConfig(Duration.ofHours(1));

        // 장소 사진 URL: TTL 24시간 (URL이 거의 변하지 않음)
        RedisCacheConfiguration placePhotoConfig = pojoCacheConfig(Duration.ofHours(24));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(searchConfig)                   // 기본값: 5분 JsonNode
                .withInitialCacheConfigurations(Map.of(
                        "flightDestinations", pojoMetaConfig,  // 24h
                        "hotelDestinations",  pojoMetaConfig,  // 24h
                        "hotelSortBy",        pojoMetaConfig,  // 24h
                        "hotelFilter",        jsonNodeMetaConfig, // 24h
                        "placeSearch",        placeSearchConfig,  // 1h
                        "placePhoto",         placePhotoConfig    // 24h
                ))
                .build();
    }
}
```

### 캐시별 TTL 요약

| 캐시 이름 | TTL | 직렬화 | 설정 근거 |
|-----------|-----|--------|-----------|
| `bookingFlights` | 5분 (default) | JsonNode | 실시간 가격·잔여석 변동 |
| `bookingFlightDetails` | 5분 (default) | JsonNode | 실시간 가격·잔여석 변동 |
| `bookingHotels` | 5분 (default) | JsonNode | 실시간 가격·잔여석 변동 |
| `flightDestinations` | 24시간 | POJO | 공항/도시 코드 거의 불변 |
| `hotelDestinations` | 24시간 | POJO | 공항/도시 코드 거의 불변 |
| `hotelSortBy` | 24시간 | POJO | 정렬 옵션 거의 불변 |
| `hotelFilter` | 24시간 | JsonNode | 필터 옵션 거의 불변 |
| `placeSearch` | 1시간 | POJO | 검색 결과 변동 가능 |
| `placePhoto` | 24시간 | POJO | 사진 URL 거의 불변 |

### `@Cacheable` 적용 예시

**항공편 검색 — 빈 결과는 캐싱하지 않음**

```java
// BookingComFlightSupplierAdapter.java
@Cacheable(
    value = "bookingFlights",
    key = "#request",
    unless = "#result == null || #result.path('data').path('flightOffers').size() == 0"
)
public JsonNode searchFlightOffers(FlightSearchRequest request) { ... }
```

**Google Places 장소 검색 및 사진 — API 키 미설정 시 graceful 처리**

```java
// GooglePlacesClient.java
@Cacheable(value = "placeSearch", key = "#text", unless = "#result == null")
public Optional<PlacesResponse> searchText(String text) {
    if (props.apiKey() == null || props.apiKey().isBlank()) {
        log.warn("Google Places API key is not configured");
        return Optional.empty();  // 키 없으면 캐싱 없이 빈 결과 반환
    }
    try {
        // WebClient 호출 ...
    } catch (WebClientResponseException e) {
        log.warn("Google Places API error: {} {}", e.getStatusCode(), e.getMessage());
        return Optional.empty();  // API 오류도 빈 결과로 처리 (예외 전파 안 함)
    }
}

@Cacheable(value = "placePhoto", key = "#photoName + '_' + #maxWidthPx", unless = "#result == null")
public Optional<PhotoMedia> getPhotoMedia(String photoName, int maxWidthPx) { ... }
```

**AI 대화 메모리 — Redis + DB 이중 캐싱**

```java
// RdbChatMemory.java
private static final Duration TTL = Duration.ofHours(24);
private static final int WINDOW_SIZE = 20;

@Override
public List<Message> get(String conversationId) {
    String cacheKey = CACHE_PREFIX + conversationId;
    try {
        ChatMemoryCache cached = (ChatMemoryCache) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("ChatMemory cache hit - chatRoomId: {}", chatRoomId);
            return cached.toMessages();
        }
    } catch (Exception e) {
        log.warn("Redis 조회 실패, DB로 fallback - chatRoomId: {}", chatRoomId, e);
    }
    // Redis miss → DB에서 최근 20개 메시지 조회 후 Redis에 24시간 캐싱
    List<ChatMessage> dbMessages = chatMessageRepository
            .findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, PageRequest.of(0, WINDOW_SIZE))
            .getContent();
    // ...
    redisTemplate.opsForValue().set(cacheKey, ChatMemoryCache.from(messages), TTL);
    return messages;
}
```

### 핵심 포인트 요약

| 항목 | 내용 |
|------|------|
| 과금 방어 | 동일 조건 재요청은 Redis에서 반환하여 외부 API 호출 차단 |
| 빈 결과 캐싱 방지 | `unless` 조건으로 빈 응답은 캐싱 제외 (다음 요청에서 실제 API 재시도) |
| API 오류 격리 | `Optional.empty()` 반환으로 외부 API 장애가 서비스 전체로 전파되지 않음 |
| 이중 캐싱 | AI 메모리는 Redis(1차) → PostgreSQL(2차) 순으로 fallback하여 Redis 장애에도 무중단 |

---

## 3. JSON 파싱 실패 예외 처리 — Fallback 및 안정적 처리

### 배경 및 문제 정의

AI(Gemini 2.5 Flash)가 여행 일정을 JSON으로 출력할 때 다음 두 가지 문제가 발생할 수 있다.

1. **형식 오염**: AI가 순수 JSON 대신 마크다운 코드 블록(` ```json ... ``` `)이나 설명 텍스트를 함께 출력
2. **파싱 실패**: JSON 구조 자체가 잘못되어 `ObjectMapper.readValue()`가 `JsonProcessingException` 발생
3. **시간 포맷 오류**: `visitTime` 필드가 `LocalTime.parse()`로 파싱 불가한 형식으로 출력

### 해결 방안: 단계적 추출 + 예외별 분기 처리

```java
// ChatService.java — AI 확정 JSON → Schedule 저장
@Transactional
public ScheduleResponse saveScheduleFromChat(Long roomId, Long workspaceId) {
    ChatMessage lastAssistantMessage = chatMessageRepository
            .findTopByChatRoomIdAndRoleOrderByCreatedAtDesc(roomId, ChatMessage.Role.ASSISTANT)
            .orElseThrow(() -> new SoflyException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

    AiScheduleOutput output;
    try {
        String content = lastAssistantMessage.getContent();

        // [Step 1] 마크다운 코드 블록 제거 — ```json ... ``` 형식 처리
        if (content.contains("```json")) {
            content = content.substring(
                content.indexOf("```json") + 7,
                content.lastIndexOf("```")
            ).trim();
        }
        // [Step 2] 앞뒤 텍스트 제거 — 중간에 JSON 객체가 포함된 경우
        else if (content.contains("{") && content.contains("}")) {
            content = content.substring(
                content.indexOf("{"),
                content.lastIndexOf("}") + 1
            ).trim();
        }

        // [Step 3] JSON 파싱 시도
        output = objectMapper.readValue(content, AiScheduleOutput.class);

    } catch (JsonProcessingException e) {
        // 파싱 실패 → 에러 화면 대신 표준 에러 코드로 응답
        // 사용자는 "일정을 다시 확정해달라"는 메시지를 받아 재시도 가능
        throw new SoflyException(ErrorCode.INVALID_AI_RESPONSE);
    }

    // [Step 4] 파싱된 구조 유효성 검증
    if (output.days() == null || output.days().isEmpty()) {
        throw new SoflyException(ErrorCode.INVALID_AI_RESPONSE);
    }

    // [Step 5] ScheduleItem 생성 — 시간 파싱 실패 시 null로 대체 (일정 전체 차단 안 함)
    List<ScheduleItemCreateRequest> items = output.days().stream()
            .flatMap(day -> day.items().stream()
                    .map(item -> new ScheduleItemCreateRequest(
                            day.day(),
                            item.orderIndex(),
                            parseVisitTime(item.visitTime()),  // 실패 시 null 반환
                            item.category(),
                            item.name(),
                            // ... 나머지 필드
                    ))
            )
            .toList();

    return scheduleService.createSchedule(new ScheduleCreateRequest(workspaceId, null, items));
}

// 방문 시간 파싱 실패 시 null로 fallback — 전체 일정 저장은 유지
private LocalTime parseVisitTime(String visitTime) {
    if (visitTime == null) return null;
    try {
        return LocalTime.parse(visitTime);
    } catch (DateTimeParseException e) {
        return null;  // 시간 정보 없이 일정 저장 진행
    }
}
```

### 외부 API 응답 JSON 파싱 실패 처리

RapidAPI 응답 파싱 실패는 `RuntimeException`으로 래핑하여 상위 레이어로 전파한다.

```java
// RapidApiJsonUtils.java
public static JsonNode parseJson(String response) {
    try {
        return OBJECT_MAPPER.readTree(response);
    } catch (JsonProcessingException e) {
        throw new RuntimeException("RapidAPI 응답 파싱 실패", e);
    }
}

// 필드 값 추출 시 null 안전 처리
public static String textOrNull(JsonNode node, String field) {
    return node.hasNonNull(field) ? node.get(field).asText() : null;
}

public static Double doubleOrNull(JsonNode node, String field) {
    return node.hasNonNull(field) ? node.get(field).asDouble() : null;
}
```

### Redis 캐시 실패 시 DB Fallback (AI 메모리)

Redis 장애가 AI 응답 생성 자체를 막지 않도록 예외를 완전히 흡수한다.

```java
// RdbChatMemory.java
@Override
public void add(String conversationId, List<Message> messages) {
    // ...DB 저장 완료 후...
    try {
        redisTemplate.delete(CACHE_PREFIX + conversationId);
    } catch (Exception e) {
        // Redis 장애 → 경고 로그만 남기고 정상 흐름 유지
        // DB 저장은 이미 완료됐으므로 데이터 유실 없음
        log.warn("Redis 캐시 삭제 실패 - chatRoomId: {}", chatRoomId);
    }
}

@Override
public List<Message> get(String conversationId) {
    try {
        ChatMemoryCache cached = (ChatMemoryCache) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached.toMessages();  // 캐시 히트
    } catch (Exception e) {
        // Redis 조회 실패 → 경고 로그 후 DB에서 직접 조회
        log.warn("Redis 조회 실패, DB로 fallback - chatRoomId: {}", chatRoomId, e);
    }
    // DB fallback: 최근 20개 메시지 조회
    // ...
}
```

### 스케줄러 재시도 패턴 (ZSet 기반)

`DataAccessException` 발생 시 ZSet 항목을 **제거하지 않아** 다음 60초 주기에 자동 재시도된다.

```java
// RedisFlightDepartureScheduler.java
} catch (DataAccessException e) {
    // ZSet에서 제거하지 않음 → 60초 후 자동 재시도
    log.warn("DB 오류로 재시도 예정: member={}, err={}", member, e.getMessage());
} catch (IllegalArgumentException e) {
    // 포맷 오류는 재시도해도 의미 없음 → 즉시 제거
    log.error("잘못된 member 포맷, 제거: member={}, err={}", member, e.getMessage());
    stringRedisTemplate.opsForZSet().remove(KEY, member);
}
```

### 예외 처리 전략 요약

| 발생 위치 | 예외 종류 | 처리 방식 |
|-----------|-----------|-----------|
| AI 응답 JSON 파싱 | `JsonProcessingException` | `SoflyException(INVALID_AI_RESPONSE)` — 에러 코드 반환, 사용자 재시도 유도 |
| 방문 시간 파싱 | `DateTimeParseException` | `null` 반환 — 시간 없이 일정 저장 진행 (부분 fallback) |
| Redis 조회/삭제 | `Exception` | 경고 로그 후 DB fallback — 서비스 중단 없음 |
| DB 오류 (스케줄러) | `DataAccessException` | ZSet 항목 유지 → 60초 후 자동 재시도 |
| 포맷 오류 (스케줄러) | `IllegalArgumentException` | ZSet 항목 즉시 제거 → 무한 재시도 방지 |
| RapidAPI 응답 파싱 | `JsonProcessingException` | `RuntimeException` 래핑 후 상위 전파 |
| Google Places API 오류 | `WebClientResponseException` | `Optional.empty()` 반환 — 외부 장애 격리 |

---

## 관련 파일 위치

```
services/travel-core-service/src/main/java/com/sofly/core/
├── domain/
│   ├── conquest/
│   │   ├── kafka/
│   │   │   └── ConquestConsumer.java              # Kafka 소비 + Redis ZSet 적재
│   │   └── scheduler/
│   │       └── RedisFlightDepartureScheduler.java # ZSet 기반 60초 주기 스케줄러
│   ├── chat/
│   │   └── service/
│   │       └── ChatService.java                   # AI JSON 파싱 + fallback 처리
│   └── workspace/
│       └── service/
│           └── WorkspaceService.java              # Kafka 이벤트 발행
└── global/
    ├── ai/
    │   └── memory/
    │       └── RdbChatMemory.java                 # Redis + DB 이중 캐싱, Redis fallback
    └── config/
        └── KafkaProducerConfig.java               # Kafka Producer 설정

services/travel-supply-service/src/main/java/com/sofly/supply/
├── config/
│   └── RedisCacheConfig.java                      # 캐시별 TTL 설정
└── adapter/outbound/
    ├── google/
    │   └── GooglePlacesClient.java                # @Cacheable + API 오류 격리
    └── rapidapi/
        ├── RapidApiJsonUtils.java                 # JSON 파싱 유틸
        └── flights/
            └── BookingComFlightSupplierAdapter.java # @Cacheable (항공 검색)
```

# feat/conquest-map-kafka-flight-departure 구현 계획

## 목표

현재 DB 전체 스캔 방식의 `VisitStatusScheduler` (30분 주기 `promotePlannedToVisited`)를  
Kafka + Redis Sorted Set 기반 이벤트 아키텍처로 교체한다.

---

## 현재 구조 (교체 대상)

| 파일 | 역할 |
|------|------|
| `domain/conquest/scheduler/VisitStatusScheduler.java` | 30분마다 DB 전체 스캔 후 PLANNED → VISITED 전환. **삭제 대상** |
| `domain/conquest/service/ConquestMapService.java` | `promotePlannedToVisited()` — DB 풀스캔. **삭제 대상 메서드** |
| `domain/workspace/service/WorkspaceService.java` | `saveFlight()` 내부에서 `ApplicationEventPublisher`로 `FlightSavedEvent` 발행 (Spring 내부 이벤트). **Kafka 발행으로 교체** |
| `domain/conquest/event/FlightSavedEvent.java` | Spring 내부 이벤트 DTO. **Kafka DTO로 대체되면 제거 가능** |
| `domain/conquest/service/ConquestMapService.java` | `onFlightSaved(FlightSavedEvent)` — `@EventListener`. **Kafka Consumer로 이전** |

---

## 목표 아키텍처

```
WorkspaceService.saveFlight()
    └─ KafkaTemplate.send("flight.saved", FlightSavedMessage)
            ├─ ConquestConsumer          → 정복 지도 PLANNED 반영 + Redis Sorted Set 등록
            ├─ NotificationConsumer      → 워크스페이스 멤버 알림 발송
            └─ WorkspaceConsumer         → 워크스페이스 일정 섹션 업데이트

RedisFlightDepartureScheduler (1분 주기)
    └─ Redis Sorted Set "flight:departures" 에서 score ≤ now 조회
            └─ ConquestMapService.promoteToVisited(userId, countryCode) 호출 후 삭제
```

---

## Kafka 메시지 구조

**토픽:** `flight.saved`

```json
{
  "workspaceId": 42,
  "memberUserIds": [1, 2, 3],
  "departureAirport": "ICN",
  "arrivalAirport": "NRT",
  "departureTime": "2026-05-01T09:00:00",
  "arrivalCountryCode": "JP"
}
```

> `arrivalCountryCode`는 `AirportInfoService.findByIata(arrivalAirport)`로 조회해서 포함하거나,  
> Consumer 측에서 직접 조회해도 무방하다.

**Redis Sorted Set 키:** `flight:departures`  
**Member 포맷:** `{userId}:{countryCode}:{cityName}`  (예: `1:JP:Tokyo`)  
**Score:** `departureTime`의 epoch seconds

---

## 구현 순서 및 수정 파일

### 1. 의존성 추가

**파일:** `build.gradle`

```groovy
// Kafka
implementation 'org.springframework.kafka:spring-kafka'
```

> Redis(`spring-boot-starter-data-redis`)는 이미 추가되어 있으므로 추가 불필요.

---

### 2. Kafka 설정

**신규 파일:** `global/config/KafkaProducerConfig.java`

```java
// KafkaTemplate<String, Object> 빈 등록
// StringSerializer (key), JsonSerializer (value)
// bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
```

**신규 파일:** `global/config/KafkaConsumerConfig.java`

```java
// ConsumerFactory<String, FlightSavedMessage> 빈 등록
// group-id: travel-core-service
// JsonDeserializer — trusted package: com.sofly.core
// 자동 오프셋 커밋 비활성화 (수동 ack 또는 기본 at-least-once)
```

**수정 파일:** `src/main/resources/application-core.yaml`

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: travel-core-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.sofly.core.*"
```

**.env에 추가 필요:**
```
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

---

### 3. Kafka 메시지 DTO

**신규 파일:** `global/kafka/dto/FlightSavedMessage.java`

```java
// 기존 FlightSavedEvent와 유사하나 Kafka 직렬화용
// 필드: workspaceId, memberUserIds, departureAirport, arrivalAirport, departureTime
// @NoArgsConstructor 필수 (Jackson 역직렬화)
```

---

### 4. FlightService (Kafka 발행으로 교체)

**수정 파일:** `domain/workspace/service/WorkspaceService.java`

- `saveFlight()` 메서드 수정
- `ApplicationEventPublisher` → `KafkaTemplate<String, FlightSavedMessage>` 교체
- `KafkaTemplate.send("flight.saved", message)` 호출
- 기존 `eventPublisher.publishEvent(new FlightSavedEvent(...))` 제거

```java
// 변경 전
eventPublisher.publishEvent(new FlightSavedEvent(memberIds, ...));

// 변경 후
kafkaTemplate.send("flight.saved", new FlightSavedMessage(workspaceId, memberIds, ...));
```

- `ApplicationEventPublisher` 주입 제거 (다른 곳에서 사용하지 않으면)

---

### 5. ConquestConsumer 구현

**신규 파일:** `domain/conquest/kafka/ConquestConsumer.java`

```java
@Component
@KafkaListener(topics = "flight.saved", groupId = "travel-core-service")
public class ConquestConsumer {

    // flight.saved 수신 시:
    // 1. AirportInfoService.findByIata(arrivalAirport) → AirportInfo 조회
    // 2. 각 memberUserId에 대해:
    //    a. ConquestMapService.applyPlannedStatus() 호출 (기존 로직 재사용)
    //    b. departureTime이 현재 이전이면 즉시 promoteToVisited() 호출
    //    c. 미래이면 Redis Sorted Set에 등록:
    //       zadd("flight:departures", epochSeconds, "{userId}:{countryCode}:{cityName}")
}
```

**수정 파일:** `domain/conquest/service/ConquestMapService.java`

- `onFlightSaved(@EventListener)` 메서드 제거
- `promotePlannedToVisited()` 메서드 제거 (DB 풀스캔 방식)
- `applyPlannedStatus()` — private → package-private 또는 public으로 접근 수준 변경 (Consumer에서 호출)
- **신규 추가 메서드:** `promoteToVisited(Long userId, String countryCode)` — 멱등성 보장

```java
@Transactional
public void promoteToVisited(Long userId, String countryCode) {
    // VisitedCountry: status == PLANNED이면 VISITED로 변경, 이미 VISITED면 스킵
    // VisitedCity: 동일 countryCode에 속한 도시들 중 PLANNED인 것 VISITED로 변경
}
```

---

### 6. NotificationConsumer 구현

**신규 파일:** `domain/workspace/kafka/NotificationConsumer.java`

```java
@Component
@KafkaListener(topics = "flight.saved", groupId = "travel-core-notification")
public class NotificationConsumer {
    // 항공편 저장 시 워크스페이스 멤버 전원에게 알림 발송 로직
    // 알림 인프라 미구현 시 로그 출력으로 stub 처리
}
```

> 알림 발송 인프라(FCM, 웹소켓 등)가 없으면 `log.info()`로 stub 처리하고 TODO 주석 남기기.

---

### 7. WorkspaceConsumer 구현

**신규 파일:** `domain/workspace/kafka/WorkspaceConsumer.java`

```java
@Component
@KafkaListener(topics = "flight.saved", groupId = "travel-core-workspace")
public class WorkspaceConsumer {
    // 항공편 저장 시 워크스페이스 일정 섹션 자동 업데이트 로직
    // 구체 스펙 미확정 시 TODO 주석으로 남기기
}
```

---

### 8. RedisFlightDepartureScheduler 구현

**신규 파일:** `domain/conquest/scheduler/RedisFlightDepartureScheduler.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFlightDepartureScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final ConquestMapService conquestMapService;

    private static final String KEY = "flight:departures";

    @Scheduled(fixedDelay = 60_000)  // 1분 주기
    public void processDeadlines() {
        double nowScore = Instant.now().getEpochSecond();

        // score ≤ now 인 항목만 조회
        Set<String> due = redisTemplate.opsForZSet()
                .rangeByScore(KEY, 0, nowScore);

        for (String member : due) {
            // member 포맷: "{userId}:{countryCode}:{cityName}"
            String[] parts = member.split(":");
            Long userId = Long.parseLong(parts[0]);
            String countryCode = parts[1];

            try {
                conquestMapService.promoteToVisited(userId, countryCode);
                redisTemplate.opsForZSet().remove(KEY, member);
                log.info("PLANNED → VISITED: userId={}, country={}", userId, countryCode);
            } catch (Exception e) {
                // 실패 시 Redis에서 삭제하지 않음 → 다음 주기 재시도
                log.warn("promoteToVisited 실패 (재시도 예정): member={}, err={}", member, e.getMessage());
            }
        }
    }
}
```

---

### 9. 기존 스케줄러 제거

**삭제 파일:** `domain/conquest/scheduler/VisitStatusScheduler.java`

- 파일 전체 삭제
- `ConquestMapService.promotePlannedToVisited()` 메서드 삭제
- `ConquestMapService.hasDepartedFlightToCountry()` private 헬퍼 삭제 (더 이상 사용 안 함)
- `ConquestMapService.onFlightSaved(@EventListener)` 삭제
- `domain/conquest/event/FlightSavedEvent.java` 삭제 (Kafka DTO로 대체)
- `WorkspaceService`의 `ApplicationEventPublisher` 의존성 제거

---

## 멱등성 및 장애 처리

| 시나리오 | 처리 방식 |
|---------|---------|
| 이미 VISITED인 항목에 promoteToVisited 호출 | status 체크 후 스킵 (DB 업데이트 없음) |
| Redis에 이미 등록된 동일 멤버 재등록 | `zadd`는 기존 score 덮어쓰기 (멱등) |
| RedisFlightDepartureScheduler 처리 실패 | Redis 항목 미삭제 → 다음 1분 주기에 재시도 |
| Kafka Consumer 실패 | Kafka 기본 재시도 보장 (at-least-once) |
| ConquestConsumer 중복 수신 | applyPlannedStatus의 UNVISITED 체크로 중복 방지 |

---

## 파일 변경 요약

### 신규 생성

```
global/config/KafkaProducerConfig.java
global/config/KafkaConsumerConfig.java
global/kafka/dto/FlightSavedMessage.java
domain/conquest/kafka/ConquestConsumer.java
domain/conquest/scheduler/RedisFlightDepartureScheduler.java
domain/workspace/kafka/NotificationConsumer.java
domain/workspace/kafka/WorkspaceConsumer.java
```

### 수정

```
build.gradle                                          — kafka 의존성 추가
src/main/resources/application-core.yaml             — kafka, 설정 추가
domain/workspace/service/WorkspaceService.java        — Kafka 발행으로 교체
domain/conquest/service/ConquestMapService.java       — 메서드 제거/추가/접근자 변경
```

### 삭제

```
domain/conquest/scheduler/VisitStatusScheduler.java   — DB 폴링 스케줄러 전체 삭제
domain/conquest/event/FlightSavedEvent.java           — Spring 내부 이벤트 DTO 삭제
```

---

## 주의사항

- `promoteToVisited`는 반드시 멱등성 보장: 이미 `VISITED`이면 DB 업데이트 없이 리턴
- Redis Sorted Set member 포맷 `{userId}:{countryCode}:{cityName}` — cityName에 `:` 포함 주의  
  → cityName 대신 `visitedCityId`(Long) 사용을 권장
- `ConquestMapService.applyPlannedStatus()`의 접근 수준을 `package-private` → `public`으로 변경 필요  
  (현재 private이므로 ConquestConsumer에서 직접 호출 불가)
- `RedisFlightDepartureScheduler`는 `StringRedisTemplate` 또는 `RedisTemplate<String, String>` 사용 권장  
  (기존 `RedisTemplate<String, Object>`와 별도 빈으로 등록 또는 `StringRedisTemplate` 주입)
- `@EnableScheduling`은 메인 클래스 또는 별도 `@Configuration`에 이미 있는지 확인 필요

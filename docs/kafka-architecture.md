# Kafka 아키텍처 가이드

> 작성일: 2026년 5월 4일

---

## 1. 개요

Sofly 백엔드에서 Kafka는 **항공편 저장 이벤트를 비동기로 여러 도메인에 전파**하기 위해 사용됩니다.

`flight.saved` 토픽 하나를 중심으로, 항공편이 워크스페이스에 저장되면 정복지도 업데이트 / 알림 발송 / 일정 자동 구성 등 세 가지 독립적인 처리 흐름이 동시에 실행됩니다. 각 Consumer가 별도 group ID를 가지므로 서로 영향을 주지 않습니다.

현재 Kafka 사용 범위는 **travel-core-service 단독**입니다. travel-supply-service는 Kafka를 사용하지 않습니다.

---

## 2. 인프라

### 2.1 Kafka 버전

| 항목 | 값 |
|---|---|
| 이미지 | `confluentinc/cp-kafka:7.6.0` |
| 모드 | KRaft (ZooKeeper 없음) |
| 노드 구성 | 단일 노드 (broker + controller) |
| 로그 보존 | 24시간 |
| 힙 메모리 | 256MB ~ 384MB |

### 2.2 로컬 개발

```bash
# local-infra/docker-compose.kafka.yml
docker compose -f local-infra/docker-compose.kafka.yml up -d
```

| 컨테이너 | 포트 | 용도 |
|---|---|---|
| `sofly-kafka` | 9092 (외부), 29092 (내부) | Kafka 브로커 |
| `sofly-kafka-ui` | 8989 | Kafka UI (provectuslabs/kafka-ui) |

Kafka UI: `http://localhost:8989`

### 2.3 프로덕션

```bash
# docker-compose.kafka.yml
docker compose -f docker-compose.kafka.yml up -d
```

프로덕션에서는 `PLAINTEXT://sofly-kafka:9092`로 advertise되며, `sofly-net` 네트워크를 통해 core-service와 통신합니다. Kafka UI는 포함되지 않습니다.

---

## 3. Spring 설정

### 3.1 application-core.yaml

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    consumer:
      group-id: travel-core-service
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

환경 변수 `KAFKA_BOOTSTRAP_SERVERS`로 주입합니다.

### 3.2 Producer 설정 (`KafkaProducerConfig`)

- Key: `StringSerializer`
- Value: `JsonSerializer`
- 타입 헤더 포함 (`ADD_TYPE_INFO_HEADERS = true`) — Consumer 측 역직렬화 타입 식별용

### 3.3 Consumer 설정 (`KafkaConsumerConfig`)

- `ConsumerFactory<String, FlightSavedMessage>` 빈 등록
- `JsonDeserializer`에 `FlightSavedMessage.class` 고정 지정
- 타입 헤더 무시 (`setUseTypeHeaders(false)`) — Producer 헤더와 무관하게 항상 `FlightSavedMessage`로 역직렬화
- 신뢰 패키지: `com.sofly.core.*`
- `ConcurrentKafkaListenerContainerFactory` 빈으로 `@KafkaListener` 컨테이너에 적용

---

## 4. 이벤트 흐름

### 4.1 토픽 목록

| 토픽 | 메시지 타입 | 방향 |
|---|---|---|
| `flight.saved` | `FlightSavedMessage` | Producer 1개 → Consumer 3개 |

### 4.2 전체 흐름도

```
[사용자: 항공편 저장 API 호출]
        │
        ▼
WorkspaceService.saveFlight()
        │
        ├── DB에 SavedFlight 저장
        │
        └── kafkaTemplate.send("flight.saved", FlightSavedMessage)
                │
                ├── [group: travel-core-service]
                │   ConquestConsumer
                │   → 도착 국가/도시 PLANNED 처리
                │   → 출발 시간 경과 시 즉시 VISITED 전환
                │   → 미래 출발 시 Redis ZSet에 등록 (스케줄러가 VISITED 전환)
                │
                ├── [group: travel-core-notification]
                │   NotificationConsumer
                │   → 멤버 알림 발송 (TODO: FCM / 웹소켓)
                │
                └── [group: travel-core-workspace]
                    WorkspaceConsumer
                    → 일정 섹션 자동 구성 (TODO: 스펙 확정 후 구현)
```

### 4.3 메시지 구조 (`FlightSavedMessage`)

```java
// com.sofly.core.global.kafka.dto.FlightSavedMessage
public class FlightSavedMessage {
    Long workspaceId;
    List<Long> memberUserIds;   // 워크스페이스 전체 멤버 ID
    String departureAirport;    // IATA 코드 (예: "ICN")
    String arrivalAirport;      // IATA 코드 (예: "NRT")
    ZonedDateTime departureTime; // KST (+09:00) 기준 출발 시각
}
```

---

## 5. Consumer 상세

### 5.1 ConquestConsumer

**파일**: `domain/conquest/kafka/ConquestConsumer.java`  
**group ID**: `travel-core-service`  
**상태**: 구현 완료

**처리 로직:**
1. `AirportInfoService.findByIata(arrivalAirport)`로 도착 공항 정보 조회
2. 워크스페이스 전체 멤버에 대해 반복:
   - `ConquestMapService.applyPlannedStatus()` — 도착 국가/도시를 `PLANNED`로 마킹
   - 출발 시각이 **현재보다 이전** → `ConquestMapService.promoteToVisited()` 즉시 `VISITED` 전환
   - 출발 시각이 **현재보다 이후** → Redis ZSet(`flight:departures`)에 `userId:countryCode:cityName`을 epoch seconds score로 등록
3. `VisitStatusScheduler`가 주기적으로 Redis ZSet을 확인해 출발 시각이 지난 항목을 `VISITED`로 일괄 전환

**Redis 키 구조:**

```
flight:departures (ZSet)
  member: "{userId}:{countryCode}:{cityName}"
  score:  출발 시각 epoch seconds (UTC)
```

### 5.2 NotificationConsumer

**파일**: `domain/workspace/kafka/NotificationConsumer.java`  
**group ID**: `travel-core-notification`  
**상태**: Stub (미구현)

항공편 저장 시 워크스페이스 멤버에게 알림을 발송합니다.  
현재는 로그만 출력하며, FCM 또는 웹소켓 알림 인프라 구현 후 실제 발송 로직으로 교체 예정입니다.

### 5.3 WorkspaceConsumer

**파일**: `domain/workspace/kafka/WorkspaceConsumer.java`  
**group ID**: `travel-core-workspace`  
**상태**: Stub (미구현)

항공편 정보를 기반으로 워크스페이스 일정 섹션을 자동 구성합니다.  
스펙 확정 후 구현 예정입니다.

---

## 6. Producer 상세

### WorkspaceService.saveFlight()

**파일**: `domain/workspace/service/WorkspaceService.java`  
**토픽**: `flight.saved`

```java
kafkaTemplate.send("flight.saved", new FlightSavedMessage(
    workspace.getId(),
    memberIds,                  // 워크스페이스 전체 멤버 ID 목록
    request.getDepartureAirport(),
    request.getArrivalAirport(),
    request.getDepartureTime()
));
```

DB 저장 완료 후 이벤트를 발행합니다. 현재는 fire-and-forget 방식으로 send 성공 여부를 별도 처리하지 않습니다.

---

## 7. 관련 파일 위치

```
services/travel-core-service/src/main/java/com/sofly/core/
├── global/
│   ├── config/
│   │   ├── KafkaProducerConfig.java        # Producer 설정
│   │   └── KafkaConsumerConfig.java        # Consumer 설정
│   └── kafka/
│       └── dto/
│           └── FlightSavedMessage.java     # 이벤트 메시지 DTO
└── domain/
    ├── conquest/
    │   └── kafka/
    │       └── ConquestConsumer.java        # 정복지도 업데이트
    └── workspace/
        ├── kafka/
        │   ├── NotificationConsumer.java   # 알림 발송 (stub)
        │   └── WorkspaceConsumer.java      # 일정 자동 구성 (stub)
        └── service/
            └── WorkspaceService.java       # Producer 호출 지점

local-infra/docker-compose.kafka.yml        # 로컬 개발용 (Kafka + UI)
docker-compose.kafka.yml                    # 프로덕션용
```

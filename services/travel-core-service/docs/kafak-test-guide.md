# Kafka 구현 로그 분석 및 테스트 가이드

## 1. 로그 분석 결과

### ✅ 정상 동작 확인

**Kafka 연결 성공**
- `bootstrap.servers = [localhost:9092]` — `.env` 설정이 정상 반영됨
- 3개 Consumer 그룹 모두 `flight.saved-0` 파티션 할당 완료

| Consumer Group | 역할 | 상태 |
|---|---|---|
| `travel-core-service` | ConquestConsumer | ✅ partitions assigned |
| `travel-core-notification` | NotificationConsumer | ✅ partitions assigned |
| `travel-core-workspace` | WorkspaceConsumer | ✅ partitions assigned |

---

### ⚠️ 초기 경고 — 무시해도 됨

```
UNKNOWN_TOPIC_OR_PARTITION
NOT_COORDINATOR / coordinator unavailable
```

**원인:** 앱 기동 직후 Kafka가 `flight.saved` 토픽을 자동 생성하는 과정에서 잠시 발생하는 정상적인 재시도 로그.  
`allow.auto.create.topics = true` 설정이 활성화되어 있어 토픽이 자동 생성된 뒤 바로 해소됨.  
최종적으로 `Successfully joined group` 메시지가 출력되면 완전히 정상.

---

### ℹ️ 참고 — Zipkin 연결 실패 (Kafka와 무관)

```
Dropped 21 spans due to ConnectException (sofly-zipkin:9411)
```

Zipkin 컨테이너가 로컬에 없어서 발생. Kafka 동작과 무관하므로 무시해도 됨.  
로컬 테스트 시 `application-core.yaml`에서 tracing을 임시 비활성화할 수 있음:
```yaml
management:
  tracing:
    sampling:
      probability: 0.0
```

---

## 2. 테스트 방법

### 사전 준비

```bash
# Kafka, Redis 실행 확인
docker ps | grep -E "sofly-kafka|redis"

# 앱 실행
./gradlew bootRun
```

---

### 테스트 1 — 항공편 저장 API 호출 (핵심 플로우)

항공편 저장 → Kafka 발행 → 3개 Consumer 수신 → PLANNED 반영 + Redis 등록

```bash
curl -X POST http://localhost:8080/api/workspaces/{workspaceId}/flights \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "KE701",
    "airline": "대한항공",
    "departureAirport": "ICN",
    "arrivalAirport": "NRT",
    "departureTime": "2026-06-01T09:00:00",
    "arrivalTime": "2026-06-01T11:30:00",
    "duration": 150,
    "price": 350000,
    "flightType": "OUTBOUND"
  }'
```

**예상 로그 (앱 콘솔)**
```
INFO  flight.saved 수신: workspaceId=1, arrival=NRT
INFO  Redis 등록: member=1:JP:도쿄, score=1748739600
INFO  [알림 stub] 항공편 저장 알림 대상: workspaceId=1, members=[1, 2]
INFO  [워크스페이스 stub] 일정 섹션 업데이트 대상: workspaceId=1
```

---

### 테스트 2 — Redis Sorted Set 등록 확인

```bash
# Redis CLI 접속
docker exec -it {redis-container-name} redis-cli

# flight:departures 키 전체 조회 (member + score)
ZRANGEBYSCORE flight:departures 0 +inf WITHSCORES
```

**예상 출력**
```
1) "1:JP:도쿄"
2) "1748739600"
```

---

### 테스트 3 — 즉시 VISITED 전환 (과거 출발 항공편)

`departureTime`을 현재 시각 이전으로 설정하면 Redis 등록 없이 즉시 VISITED로 전환됨.

```bash
curl -X POST http://localhost:8080/api/workspaces/{workspaceId}/flights \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "OZ101",
    "airline": "아시아나",
    "departureAirport": "ICN",
    "arrivalAirport": "LAX",
    "departureTime": "2025-01-01T09:00:00",
    "arrivalTime": "2025-01-01T22:00:00",
    "duration": 600,
    "price": 1200000,
    "flightType": "OUTBOUND"
  }'
```

**예상 로그**
```
INFO  즉시 VISITED 전환: userId=1, country=US
```

**정복 지도 DB 확인**
```sql
-- visited_country 테이블에서 US가 VISITED인지 확인
SELECT * FROM visited_country WHERE user_id = 1 AND country_code = 'US';
```

---

### 테스트 4 — 스케줄러 동작 확인 (1분 대기)

미래 출발 항공편을 등록한 뒤, `departureTime`이 지나면 스케줄러가 자동으로 PLANNED → VISITED 전환.

빠른 테스트를 위해 `departureTime`을 1~2분 후로 설정:

```bash
# 현재 시각 + 1분으로 departureTime 설정
-d '{ "departureTime": "2026-04-27T01:35:00", ... }'
```

1분 후 로그 확인:
```
INFO  출발 도래 항목 1건 처리 시작
INFO  국가 PLANNED→VISITED: userId=1, country=JP
INFO  도시 PLANNED→VISITED: userId=1, city=도쿄
INFO  PLANNED → VISITED 완료: userId=1, country=JP
```

Redis에서 해당 member 제거 확인:
```bash
ZRANGEBYSCORE flight:departures 0 +inf WITHSCORES
# 빈 결과 나오면 정상
```

---

### 테스트 5 — Kafka UI로 메시지 확인

브라우저에서 `http://localhost:8989` 접속

1. `Topics` → `flight.saved` 선택
2. `Messages` 탭에서 발행된 메시지 JSON 확인

**예상 메시지 구조**
```json
{
  "workspaceId": 1,
  "memberUserIds": [1, 2],
  "departureAirport": "ICN",
  "arrivalAirport": "NRT",
  "departureTime": "2026-06-01T09:00:00"
}
```

---

### 테스트 6 — 멱등성 확인

같은 항공편을 두 번 저장해도 DB 상태가 중복 변경되지 않아야 함.

```bash
# 동일 요청 2회 반복
curl -X POST http://localhost:8080/api/workspaces/{workspaceId}/flights ...
curl -X POST http://localhost:8080/api/workspaces/{workspaceId}/flights ...
```

**확인 포인트**
- `visited_country` 테이블에 동일 `(user_id, country_code)` 레코드가 1개만 존재
- 이미 PLANNED인 항목에 재등록 시 상태 변경 없음 (로그에 update 없어야 함)
- Redis `zadd`는 기존 score를 덮어쓰므로 중복 등록 없음

---

## 3. 트러블슈팅

| 증상 | 원인 | 해결 |
|---|---|---|
| `UnknownHostException: sofly-kafka` | ADVERTISED_LISTENERS 호스트명 불일치 | docker-compose PLAINTEXT → localhost:9092 수정 |
| `UNKNOWN_TOPIC_OR_PARTITION` (시작 직후) | 토픽 자동 생성 지연 | 정상, 수초 내 자동 해소 |
| Consumer가 메시지를 수신 못함 | `containerFactory` 빈 이름 불일치 | `@KafkaListener(containerFactory = "kafkaListenerContainerFactory")` 확인 |
| Redis에 member가 안 쌓임 | `StringRedisTemplate` 빈 미주입 | `RedisConfig`에 `StringRedisTemplate` 빈 추가 또는 Spring Boot 자동 등록 확인 |
| `promoteToVisited` 트랜잭션 오류 | `@Transactional` self-invocation | Consumer에서 직접 호출하므로 문제 없음 (별도 빈) |

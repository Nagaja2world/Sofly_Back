# Sofly 백엔드 포트폴리오

> AI 기반 그룹 여행 플래닝 서비스 Sofly의 백엔드 설계 및 구현 기록

---

## 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **서비스명** | Sofly |
| **설명** | 친구·가족과 함께 여행을 계획하고, AI가 맞춤 일정을 추천해주는 그룹 여행 플래닝 서비스 |
| **역할** | 백엔드 개발 (2인 팀 — 박상민, 정세현) |
| **개발 기간** | 2026년 3월 ~ 2026년 6월 |
| **서비스 URL** | https://api.sofly.co.kr |
| **GitHub** | https://github.com/sm010422/Sofly_Back |
| **기술 스택** | Java 21 / Spring Boot 3.5 / PostgreSQL / Redis / Kafka / AWS / Docker |

---

## 서비스 주요 기능

| 기능 | 설명 |
|------|------|
| **소셜 로그인** | Google / Kakao / Naver OAuth2 + JWT Stateless 인증 |
| **그룹 워크스페이스** | 초대 코드로 멤버 초대, 역할(OWNER/EDITOR/VIEWER) 기반 권한 제어 |
| **AI 여행 플래너 채팅** | Google Gemini 2.5 Flash 기반 3단계 여행 추천 (정보 수집 → 자연어 제안 → JSON 확정) |
| **항공권·호텔 검색** | Booking.com RapidAPI 실시간 연동, Redis 캐싱으로 응답 최적화 |
| **장소 정보 조회** | Google Places API 기반 텍스트 검색 및 사진 제공 |
| **여행 일정 관리** | 일정표 생성·편집·포크(복제), 카테고리별 아이템 관리 |
| **여행 앨범** | AWS S3 기반 사진 업로드·관리 (최대 20장/요청) |
| **여행 로그** | Markdown 기반 여행 기록 (공개/멤버/비공개 설정) |
| **정복 지도** | 방문 국가·도시 추적 (UNVISITED → PLANNED → VISITED), 공항 이벤트 자동 전환 |
| **실시간 채팅** | WebSocket STOMP + Redis Pub/Sub 기반 멀티 인스턴스 브로드캐스트 |

---

## 시스템 아키텍처

### 전체 구성

이 프로젝트는 **Gradle 멀티모듈 모노레포** 위에 두 개의 독립 배포 서비스로 구성됩니다.

```
┌────────────────────────────────────────────────────────────┐
│                     Client (Frontend)                       │
└──────────────────┬────────────────────────┬────────────────┘
                   │ HTTPS / WebSocket       │
                   ▼                         ▼
┌──────────────────────────┐   ┌──────────────────────────────┐
│   travel-core-service    │   │   travel-supply-service       │
│   레이어드 아키텍처       │   │   헥사고날 아키텍처           │
│   :8080                  │   │   :8082 (Docker)              │
│                          │   │                              │
│ - 인증 / 사용자           │   │ - 항공권 검색                │
│ - 워크스페이스 / 멤버     │   │ - 호텔 검색                  │
│ - AI 여행 플래너 채팅     │   │ - 장소 정보 (Places)         │
│ - 일정 / 앨범 / 여행 로그 │   │                              │
│ - 정복 지도               │   │ Booking.com (RapidAPI)       │
│ - WebSocket 채팅          │   │ Google Places API            │
└──────────────────────────┘   └──────────────────────────────┘
             │                                │
             └────────────────┬───────────────┘
                              ▼
              ┌───────────────────────────────┐
              │  PostgreSQL  │  Redis  │ Kafka │
              │  (도메인 DB)  │ (캐시/토큰) │ (이벤트) │
              └───────────────────────────────┘
```

### 서비스별 아키텍처 패턴 선택 이유

| 서비스 | 패턴 | 선택 이유 |
|--------|------|-----------|
| `travel-core-service` | 레이어드 아키텍처 | 도메인이 7개(인증·워크스페이스·일정·채팅·앨범·여행 로그·정복 지도)로 복잡하고 도메인 로직 중심이므로 계층 분리 명확화 |
| `travel-supply-service` | 헥사고날 아키텍처 | 외부 공급사 교체 가능성이 높아 Port 인터페이스로 공급사 구현체를 추상화하고, `SupplierRegistry`로 런타임 교체 지원 |

---

## 기술 스택

### 백엔드 코어

| 분류 | 기술 |
|------|------|
| 언어 / 프레임워크 | Java 21, Spring Boot 3.5.x |
| 웹 | Spring Web MVC (REST API), Spring WebSocket (STOMP) |
| 보안 / 인증 | Spring Security (Stateless), JWT, OAuth2 (Google / Kakao / Naver) |
| ORM / 데이터 | Spring Data JPA, Hibernate |
| AOP | `@RequireWorkspaceMember` — 워크스페이스 권한 검사 커스텀 어노테이션 |
| 서비스 간 통신 | Spring Cloud OpenFeign |

### 데이터 저장

| 기술 | 용도 |
|------|------|
| PostgreSQL | 운영 DB (도메인 엔티티 전체) |
| Redis | Refresh Token, AI 채팅 메모리 캐시(24h TTL), 외부 API 캐싱, ZSet 기반 출발 일정 관리 |
| MongoDB | 문서형 데이터 저장 |
| H2 | 테스트 인메모리 DB |

### 외부 서비스

| 서비스 | 용도 |
|--------|------|
| Google Gemini 2.5 Flash (Spring AI) | AI 여행 플래너 채팅 |
| Google Places API | 장소 텍스트 검색, 사진 URL 제공 |
| Booking.com (RapidAPI) | 항공권·호텔 실시간 검색 |
| AWS S3 / Cloudflare R2 | 앨범 사진 오브젝트 스토리지 |
| Google Calendar API | 캘린더 연동 |

### 메시징 / 실시간

| 기술 | 용도 |
|------|------|
| Apache Kafka (KRaft, 단일 노드) | 도메인 이벤트 비동기 처리 (`flight.saved`, `workspace.invitation` 토픽) |
| WebSocket STOMP | 실시간 채팅 |
| Redis Pub/Sub | 멀티 인스턴스 브로드캐스트 (Blue-Green 환경 대응) |

### 인프라 / DevOps

| 기술 | 용도 |
|------|------|
| Docker (멀티스테이지 빌드) | 서비스 컨테이너화, 이미지 경량화 |
| Nginx | Blue-Green 무중단 배포 리버스 프록시 |
| GitHub Actions | 서비스별 독립 CI/CD 파이프라인 |
| AWS Lightsail | 프로덕션 서버 |

### 관측성 (Observability)

| 기술 | 역할 |
|------|------|
| Prometheus + Micrometer | 메트릭 수집 (`/actuator/prometheus` 스크레이핑) |
| Grafana | 메트릭 시각화 대시보드 |
| Zipkin + Brave | 분산 트레이싱 |
| cAdvisor | 컨테이너별 CPU·메모리 모니터링 |
| Dozzle | 컨테이너 로그 실시간 조회 |

---

## 핵심 기술 구현 & 트러블슈팅

### 1. 정복 지도 상태 전환 — DB 풀스캔에서 Kafka + Redis ZSet 기반 이벤트 아키텍처로

**문제 상황**

초기 설계는 30분마다 `visited_country`, `visited_city` 테이블 전체를 스캔하여 PLANNED → VISITED 전환하는 스케줄러였다. 이 방식은 세 가지 문제를 갖고 있었다.

- 사용자가 늘수록 매 실행마다 테이블 전체를 읽어야 하는 **DB 풀스캔**
- 출발일이 아직 먼 항목도 매번 조회하는 **불필요한 쿼리 낭비**
- `saveFlight()` 내부에서 `ApplicationEventPublisher`로 동기 처리하여 알림 발송 실패 시 지도 업데이트까지 롤백되는 **장애 전파 위험**

**해결**

항공편 저장 시점에 Kafka에 이벤트를 발행하고, Consumer가 Redis ZSet에 출발 시각을 score로 등록한다. 스케줄러는 1분마다 `score ≤ now`인 항목만 꺼내 VISITED로 전환한다.

```
항공편 저장 → KafkaTemplate.send("flight.saved")
  └─ ConquestConsumer (group: travel-core-service)
      ├─ 도착지 PLANNED 마킹
      ├─ 출발 시각 < now  → 즉시 VISITED 전환
      └─ 출발 시각 > now  → Redis ZSet 등록
                            zadd("flight:departures", epochSeconds, "{userId}:{countryCode}:{cityName}")

RedisFlightDepartureScheduler (1분 주기)
  └─ zrangeByScore("flight:departures", 0, now) → VISITED 전환 후 ZSet 제거
```

| 항목 | 기존 (DB 풀스캔) | 변경 후 (Redis ZSet) |
|------|----------------|---------------------|
| 스캔 범위 | 전체 PLANNED 레코드 | score ≤ now 항목만 |
| 처리 주기 | 30분 | 1분 |
| 이벤트 처리 | 동기, 단일 트랜잭션 | Kafka 비동기, Consumer 그룹별 독립 처리 |
| 장애 격리 | 알림 실패 시 지도 업데이트도 롤백 위험 | Consumer 그룹 분리로 독립 실패/재시도 |

Consumer group ID를 분리(`travel-core-service`, `travel-core-notification`, `travel-core-workspace`)하여 알림 발송, 일정 자동 구성 등 후속 처리를 정복 지도 업데이트와 완전히 독립적으로 붙이고 떼어낼 수 있는 구조로 만들었다.

---

### 2. 항공편 목적지 자동완성 — 입력 길이 조건부 캐싱

**문제 상황**

사용자가 "인천공항"을 타이핑하는 과정에서 `"ㅇ"`, `"인"`, `"인천"`, `"인천공"`, `"인천공항"` 각 단계마다 RapidAPI가 호출됐다. 월간 쿼터가 있는 외부 API에서 의미 없는 1글자 쿼리가 모두 소비되고, Redis에 TTL 24시간짜리 단편 키가 쌓였다.

**해결**

```java
@Cacheable(
    value = "flightDestinations",
    key = "#query.toLowerCase().trim() + ':' + #languageCode",
    condition = "#query != null && #query.trim().length() >= 2",
    unless = "#result == null || #result.isEmpty()"
)
public List<FlightDestination> searchDestinations(String query, String languageCode) {
    if (query == null || query.trim().length() < 2) {
        return List.of();  // API 호출 없이 빈 결과 반환
    }
    // ...
}
```

- `condition`: 2글자 미만 쿼리는 캐싱과 API 호출 모두 차단
- 키 정규화(`toLowerCase().trim()`): "ICN"과 "icn"이 동일 캐시 엔트리 공유
- `unless`: 빈 결과는 캐싱 제외 — 일시적 API 오류가 캐시에 굳는 현상 방지

---

### 3. Google Places API — 검색·사진 분리 캐싱

**문제 상황**

Google Places API는 텍스트 검색(`POST /v1/places:searchText`)과 사진 URL 조회(`GET /v1/{photoName}/media`) 두 단계로 구성된다. 장소 카드 한 장을 렌더링할 때 검색 1회 + 사진 수만큼 추가 API 호출이 발생했다. 월 200달러 무료 크레딧이 예상보다 빠르게 소진됐다.

**해결**

용도별로 TTL과 직렬화 방식을 분리하여 캐싱했다.

```java
// 장소 검색: 1시간 (실시간성과 비용 절감 균형)
@Cacheable(value = "placeSearch", key = "#text", unless = "#result == null")
public Optional<PlacesResponse> searchText(String text)

// 사진 URL: 24시간 (photoName 기준 URL은 장기간 변경 없음)
@Cacheable(value = "placePhoto", key = "#photoName + '_' + #maxWidthPx", unless = "#result == null")
public Optional<PhotoMedia> getPhotoMedia(String photoName, int maxWidthPx)
```

`RedisCacheConfig`에서 각 캐시의 TTL과 직렬화 방식(`GenericJackson2JsonRedisSerializer`)을 명시적으로 등록하고, 검색 키에 `normalizeSearchText()`를 적용하여 유니코드 공백 차이로 인한 캐시 미스를 방지했다.

---

### 4. 앨범 업로드 — Presigned URL에서 서버 측 multipart 전환

**문제 상황**

초기 설계인 Presigned URL 방식은 클라이언트에서 URL 발급 → S3 직접 업로드 → DB 저장의 3단계를 모두 처리해야 했다.

- 업로드 중단 시 S3에는 올라갔으나 DB에는 저장 안 된 **불일치 상태** 발생 가능
- 서버를 경유하지 않으므로 파일 타입·크기·개수 **유효성 검증 불가**
- 프론트엔드 구현 복잡도 증가

**해결**

서버가 `multipart/form-data`로 파일을 수신하여 검증 → S3 업로드 → DB INSERT를 **단일 트랜잭션**으로 처리하는 방식으로 전환했다.

```
클라이언트 → POST /api/album/{albumId}/photos (multipart/form-data)
              └─ 서버: 파일 검증 (타입·크기·개수) → S3Service.uploadFile() → DB INSERT
```

트레이드오프로 파일이 서버를 경유하므로 네트워크 트래픽이 증가하지만(최대 10MB × 20개), 현재 트래픽 규모에서는 허용 가능한 수준으로 판단했다.

---

### 5. JPA 성능 문제 — N+1 및 Cartesian Product 해결

**N+1 문제 (앨범)**

앨범 사진 목록 조회 시 Lazy 로딩으로 사진마다 업로더(`uploadedBy`) 정보를 별도 쿼리로 조회하는 N+1이 발생했다. `JOIN FETCH` 쿼리 메서드로 단일 쿼리에서 함께 로드하도록 수정했다.

**Cartesian Product 문제 (여행 로그)**

TravelLog 목록 조회에서 `photos`를 `LEFT JOIN FETCH`로 함께 불러오면, 로그 한 건에 사진이 여러 개 붙을 때 사진 수만큼 row가 중복되는 Cartesian Product가 발생했다.

목록 조회에서 실제로 필요한 것은 `photoCount`뿐이었다. JPQL constructor projection과 `SIZE()` 서브쿼리로 교체하여 불필요한 JOIN을 제거했다.

```java
@Query("""
    SELECT new com.sofly.core.domain.travellog.dto.TravellogSummaryResponse(
        t.id, t.title, t.visibility, t.createdAt, SIZE(t.photos))
    FROM TravelLog t WHERE t.workspace.id = :workspaceId
""")
```

---

### 6. Timezone 처리 버그 — LocalDateTime에서 ZonedDateTime으로

**문제 상황**

Kafka Consumer에서 항공편 출발 시각(KST +09:00)을 `LocalDateTime`으로 처리했는데, 서버가 UTC 기준 `LocalDateTime.now()`와 비교하면서 9시간 오차가 발생했다. KST 09:00 출발 항공편이 UTC 00:00 출발임에도 아직 출발하지 않은 것으로 판단하는 케이스가 생겼다.

**해결**

`departureTime` 타입을 `ZonedDateTime`으로 변경하고, Kafka DTO·요청 DTO·Consumer 비교 로직을 모두 `Instant` 기준으로 통일했다.

```java
// 변경 전
if (departureTime.isBefore(LocalDateTime.now(ZoneOffset.UTC)))

// 변경 후
if (message.getDepartureTime().toInstant().isBefore(Instant.now()))
```

---

### 7. WebSocket STOMP — JWT 인증 + Redis Pub/Sub 브로드캐스트

**문제 상황**

WebSocket STOMP 연결은 HTTP 핸드셰이크 이후 별도 프로토콜로 동작하여 기존 `JwtAuthenticationFilter`가 적용되지 않았다. 인증 없이 아무나 채팅방에 메시지를 보낼 수 있는 상태였다.

또한 Blue-Green 배포 환경에서 인스턴스가 두 개 올라가면 `SimpMessagingTemplate`이 메시지를 발행한 인스턴스에 연결된 사용자에게만 전달하는 문제가 예상됐다.

**해결**

`StompChannelInterceptor`를 구현하여 `CONNECT` 프레임 수신 시 STOMP 헤더에서 JWT를 추출·검증하고, 인증된 사용자 정보를 STOMP 세션에 저장했다. 이후 `SEND` 프레임에서 세션 정보로 신원을 확인한다.

메시지 발행을 `SimpMessagingTemplate` 직접 호출 대신 Redis `ChannelTopic`에 publish하고, 모든 인스턴스의 `RedisSubscriber`가 해당 채널을 구독하여 자신에게 연결된 사용자에게 전달한다. 어떤 인스턴스에 연결된 사용자도 메시지를 수신할 수 있다.

---

### 8. AI 채팅 — Spring.toList() 불변 리스트 런타임 오류

**문제 상황**

Spring AI의 `RdbChatMemory`에서 DB 조회 결과를 `Stream.toList()`로 반환했는데, Java 16+의 `Stream.toList()`는 **불변 리스트**를 반환한다. Spring AI 내부의 `MessageAggregator`가 이 리스트에 `.add()`를 호출하면서 `UnsupportedOperationException`이 런타임에 발생했다.

**해결**

`Stream.toList()` → `Collectors.toList()`로 변경하여 mutable `ArrayList`가 반환되도록 했다. Spring AI 내부 구현 상세를 설계 단계에서 파악하지 못해 놓친 케이스였다.

---

### 9. 헥사고날 아키텍처 기반 공급사 교체 — Amadeus에서 Booking.com으로

**문제 상황**

초기 공급사로 선택한 Amadeus API는 세 가지 문제가 있었다.

- OAuth2 Client Credentials 방식의 Token 발급·갱신 로직 복잡성
- GDS 기반 항공 업계 응답 포맷 파싱 비용
- 테스트 환경 데이터가 실제 운항 스케줄과 달라 프론트엔드 연동 검증 어려움

**해결**

`FlightSupplierPort` / `HotelSupplierPort` 인터페이스를 구현하는 Booking.com 어댑터를 작성하고, `SupplierRegistry`의 기본 키를 `amadeus` → `booking`으로 변경하는 것만으로 공급사 전환을 완료했다. `supplier=<key>` 쿼리 파라미터로 런타임에 공급사를 선택할 수 있어 향후 멀티 공급사 지원도 가능하다.

헥사고날 아키텍처가 외부 의존성 교체 비용을 실질적으로 낮춘 사례다.

---

## CI/CD 파이프라인 — Blue-Green 무중단 배포

### 전체 흐름

```
develop 브랜치 push
  └─ GitHub Actions (ubuntu-latest)
       ├─ Docker 멀티스테이지 빌드
       │   ├─ 1단계: eclipse-temurin:21-jdk-alpine + gradlew bootJar → JAR 생성
       │   └─ 2단계: eclipse-temurin:21-jre-alpine + JAR만 복사 (경량 이미지)
       └─ Docker Hub push (태그: {github.sha})
             └─ AWS Lightsail 서버 (SSH)
                  ├─ docker pull
                  ├─ 새 컨테이너 실행 (Green)
                  ├─ /actuator/health 헬스체크
                  ├─ Nginx .inc 파일 포트 변경 + reload (트래픽 전환, 무중단)
                  └─ 이전 컨테이너(Blue) 종료
```

**멀티스테이지 Dockerfile의 이점**

| 항목 | 기존 방식 | 현재 방식 |
|------|---------|---------|
| 빌드 횟수 | 2번 (Actions + Docker) | 1번 (Docker 내부 단독) |
| JDK 설치 | GitHub Actions에 필요 | Dockerfile에 포함, 불필요 |
| 최종 이미지 크기 | JDK 포함 (大) | JRE만 포함 (소) |
| src 변경 시 의존성 캐시 | 매번 재다운로드 | 의존성 레이어 캐시 재사용 |

**헬스체크 실패 시 자동 롤백**

```bash
rollback() {
  # Nginx를 이전 Blue 포트로 복구
  echo 'set $core_url http://127.0.0.1:${CURRENT_PORT};' | \
    sudo tee /etc/nginx/bluegreen/service-url-core.inc
  sudo nginx -s reload

  # 실패한 Green 컨테이너 정리
  sudo docker stop core-green && sudo docker rm core-green
}
```

배포 실패 시 서비스 다운타임 없이 이전 Blue 컨테이너로 즉시 복구된다. 이미지 태그를 `{github.sha}`로 관리하여 특정 커밋 시점으로의 롤백도 가능하다.

---

## 모니터링 스택

운영 중 컨테이너 상태와 캐시 동작을 파악할 수단이 없어 디버깅이 어려운 문제로 인해, 설계 계획에는 없었던 모니터링 스택을 별도 `docker-compose`로 구성했다.

| 도구 | 역할 |
|------|------|
| Prometheus | core/supply 서비스 메트릭 수집 (`/actuator/prometheus`) |
| Grafana | 대시보드 시각화 (HikariCP 커넥션 풀, Redis 히트율, 응답 시간) |
| cAdvisor | 컨테이너별 CPU·메모리 사용량 모니터링 |
| Redis Exporter | Redis 커넥션 수, 히트율, 메모리 사용량 |
| Zipkin | core ↔ supply 서비스 간 분산 트레이싱 |
| Dozzle | 컨테이너 로그 실시간 조회 (Nginx 역방향 프록시 경유) |

> cAdvisor CPU 과사용 문제는 `housekeeping_interval`을 15초로 조정하여 해결했다.

---

## 설계 의사결정 요약 (ADR)

| ADR | 결정 | 이유 |
|-----|------|------|
| ADR-01 | 정복 지도: DB 풀스캔 → Kafka + Redis ZSet | DB 풀스캔 확장성 한계, 이벤트 처리 독립성 확보 |
| ADR-02 | 항공편 목적지 캐싱: 조건부 (`length >= 2`) | 1글자 쿼리로 인한 API 쿼터 낭비 및 Redis 메모리 낭비 방지 |
| ADR-03 | Google Places: 검색(1h) / 사진(24h) 분리 캐싱 | API 크레딧 소진 속도 실측 후 용도별 TTL 차별화 |
| ADR-04 | 앨범 업로드: Presigned URL → 서버 측 multipart | 클라이언트 복잡도, 검증 불가, S3/DB 불일치 위험 해소 |
| ADR-05 | 공급사: Amadeus → Booking.com RapidAPI | Token 관리 복잡성, 응답 파싱 비용, 테스트 데이터 품질 문제 |

---

## 개발 회고

### 설계와 구현 사이의 간격

설계 단계에서 "단순한 것 먼저"로 미뤘던 캐싱, 이벤트 처리, 파일 업로드 방식이 실제 구현에서 모두 재설계 대상이 됐다. 외부 API 호출 비용, 프론트엔드 복잡도, 장애 전파 경로는 실제로 연결해보기 전까지 명확하게 보이지 않는다는 것을 체감했다. ADR을 남기는 습관이 "왜 이렇게 했는지"를 팀 내에서 공유하는 데 유용했다.

### 아키텍처 패턴의 실용적 가치

헥사고날 아키텍처에서 `FlightSupplierPort` 인터페이스 덕분에 Amadeus → Booking.com 전환이 단 하나의 기본 키 변경으로 완료됐다. 당시에는 과한 추상화처럼 보였지만, 실제 공급사 교체 시 그 가치를 경험했다. 반대로 core-service는 레이어드 아키텍처로도 도메인 경계를 패키지로 구분하여 유사한 분리 효과를 얻었다.

### 모니터링은 개발 초기부터

개발 중반에 모니터링 스택을 붙이고 나서야 Redis 캐시 히트율, 컨테이너 메모리 사용 패턴, 서비스 간 호출 지연이 실수로 보이기 시작했다. Prometheus + Grafana를 처음부터 구성했더라면 ADR-02(캐싱 전략 세분화)를 더 빨리 발견할 수 있었을 것이다.

---

## 프로젝트 구조

```
Sofly_Back/
├── services/
│   ├── travel-core-service/       # 핵심 비즈니스 로직 (:8080)
│   │   └── src/main/java/com/sofly/core/
│   │       ├── global/            # 공통 (보안, AI, 예외, 응답 포맷)
│   │       └── domain/            # 7개 비즈니스 도메인
│   │           ├── user/
│   │           ├── workspace/     # Kafka Producer 포함
│   │           ├── schedule/
│   │           ├── chat/          # AI 채팅, WebSocket
│   │           ├── album/         # S3 연동
│   │           ├── travellog/
│   │           └── conquest/      # Kafka Consumer, Redis ZSet 스케줄러
│   │
│   └── travel-supply-service/     # 외부 공급사 API 연동 (:8082 Docker)
│       └── src/main/java/com/sofly/supply/
│           ├── adapter/           # Inbound(REST) / Outbound(Google, RapidAPI)
│           ├── application/       # Port 인터페이스, Service, DTO
│           ├── bootstrap/         # SupplierRegistry (공급사 자동 탐지)
│           └── config/            # WebClient, Redis 캐시 설정
│
├── docs/                          # 운영 문서
├── docker-compose.yml             # 프로덕션
├── docker-compose.local.yml       # 로컬 인프라 전체 스택
└── .github/workflows/             # 서비스별 독립 CI/CD
    ├── deploy-core.yml
    └── deploy-supply.yml
```

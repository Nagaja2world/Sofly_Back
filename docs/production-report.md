# Sofly 백엔드 제작 보고서

> 작성일: 2026년 5월 16일

---

## 목차

1. [제작 내용 — 기술적 문제와 해결 과정](#1-제작-내용--기술적-문제와-해결-과정)
2. [설계 변경 근거 (ADR 이력)](#2-설계-변경-근거-adr-이력)
3. [프로젝트 수행 계획 대비 실행 현황](#3-프로젝트-수행-계획-대비-실행-현황)

---

## 1. 제작 내용 — 기술적 문제와 해결 과정

### 1-1. 정복지도 PLANNED → VISITED 상태 전환: DB 풀스캔 → Redis ZSet + Kafka

#### 설계 단계의 접근

상세 설계 단계에서는 "항공편 출발일이 지난 PLANNED 상태 데이터를 VISITED로 전환"하는 로직을 다음과 같이 설계했다.

```
VisitStatusScheduler (30분 주기 @Scheduled)
  └─ ConquestMapService.promotePlannedToVisited()
      ├─ SELECT * FROM visited_country WHERE status = 'PLANNED'
      ├─ SELECT * FROM visited_city WHERE status = 'PLANNED'
      └─ 각 항목의 연관 항공편 출발 시각 조회 후 현재 시각과 비교 → VISITED 전환
```

직관적이고 구현이 단순하다는 장점이 있었다.

#### 구현 중 발견된 문제

실제 구현 과정에서 다음 문제들이 드러났다.

1. **DB 풀스캔**: 사용자가 늘수록 `visited_country`, `visited_city` 테이블 전체를 30분마다 스캔한다. 출발일이 아직 멀리 남은 `PLANNED` 항목도 매 실행마다 읽고 비교한다.
2. **불필요한 항공편 조인**: 각 PLANNED 항목에 대해 연관 `SavedFlight`의 출발 시각을 조회하는 추가 쿼리가 필요하다. N+1 문제가 발생하거나, JOIN 쿼리로 묶더라도 스케줄 실행마다 전량 처리가 강제된다.
3. **항공편 저장 이벤트와의 단절**: `saveFlight()` 호출 시 Spring 내부 이벤트(`ApplicationEventPublisher`)로 정복지도를 업데이트했는데, 이 방식은 동일 트랜잭션 내에서 동기로 실행되어 알림 발송, 일정 자동 구성 등 후속 처리를 붙이기 어렵고, 한 처리의 실패가 다른 처리를 막는 구조였다.

#### 해결: Kafka + Redis Sorted Set 기반 이벤트 아키텍처로 전환

항공편 저장 시점에 **필요한 정보만 Redis에 예약**해두고, **스케줄러는 "지금 처리할 것"만 꺼내는** 방식으로 전환했다.

```
항공편 저장 시점:
WorkspaceService.saveFlight()
  └─ KafkaTemplate.send("flight.saved", FlightSavedMessage)
        └─ ConquestConsumer (group: travel-core-service)
            ├─ 도착지 → PLANNED 마킹
            ├─ 출발 시각 < now  → 즉시 VISITED 전환
            └─ 출발 시각 > now  → Redis ZSet 등록
                                  zadd("flight:departures", epochSeconds, "{userId}:{countryCode}:{cityName}")

VISITED 전환 시점:
RedisFlightDepartureScheduler (1분 주기)
  └─ zrangeByScore("flight:departures", 0, now) → score ≤ now 인 항목만 조회
      └─ ConquestMapService.promoteToVisited(userId, countryCode) → VISITED 전환 후 ZSet에서 제거
```

**핵심 개선점:**

| 항목 | 기존 (DB 풀스캔) | 변경 후 (Redis ZSet) |
|------|----------------|---------------------|
| 스캔 범위 | 전체 PLANNED 레코드 | score ≤ now 인 항목만 |
| 처리 주기 | 30분 | 1분 (지연 최소화) |
| 항공편 이벤트 처리 | 동기, 단일 트랜잭션 | Kafka로 비동기 분산, Consumer 그룹별 독립 처리 |
| 장애 격리 | 알림 실패 시 지도 업데이트도 롤백 위험 | Consumer 그룹 분리로 독립 실패/재시도 |

Kafka Consumer를 group ID 기준으로 분리했기 때문에 향후 알림(`travel-core-notification`), 일정 자동 구성(`travel-core-workspace`) 등의 처리를 정복지도 업데이트와 독립적으로 붙이고 떼어낼 수 있다.

---

### 1-2. 항공편 목적지 검색 API: 단순 캐싱 → 입력 길이 조건부 캐싱

#### 설계 단계의 접근

항공편 검색을 위한 공항/도시 자동완성 API(`/api/v1/flights/searchDestination`)에 대해 설계 단계에서는 "동일 쿼리에 대한 중복 API 호출 방지"를 목적으로 단순 캐싱을 적용하기로 했다.

```java
// 초기 설계
@Cacheable(value = "flightDestinations", key = "#query + ':' + #languageCode")
public List<FlightDestination> searchDestinations(String query, String languageCode)
```

#### 구현 중 발견된 문제

실제 프론트엔드와 연동하면서 문제가 드러났다. 사용자가 "인천공항"을 검색하는 과정에서 `"ㅇ"`, `"인"`, `"인천"`, `"인천공"`, `"인천공항"` 각 단계마다 API가 호출되었다. 이로 인해 발생한 문제는 두 가지였다.

1. **API 사용량 낭비**: RapidAPI는 월간 호출 횟수 쿼터가 존재한다. 의미 없는 1글자 쿼리가 모두 외부 API를 소비한다.
2. **캐시 키 폭발**: `"ㅇ:ko"`, `"인:ko"` 등 의미 없는 짧은 쿼리가 Redis에 TTL 24시간으로 적재되어 캐시 메모리를 낭비한다.

#### 해결: 최소 입력 길이 조건 추가

`@Cacheable`의 `condition` 속성으로 2글자 미만 쿼리는 캐싱과 API 호출 모두 차단했다.

```java
// 변경 후
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

캐시 키도 `#query.toLowerCase().trim()`으로 정규화하여 "ICN"과 "icn"이 동일 캐시 엔트리를 공유하도록 했다. `unless` 조건으로 빈 결과는 캐싱하지 않아 일시적인 API 오류가 캐시에 굳는 것도 방지했다.

---

### 1-3. Google Places API: 캐싱 미적용 → 검색/사진 분리 캐싱

#### 설계 단계의 접근

초기 설계에서 `GooglePlacesClient`는 캐싱 없이 매 요청마다 Google Places API를 직접 호출하는 구조였다. 외부 API이므로 캐싱이 필요하다는 인식은 있었지만, 설계 단계에서는 "일단 동작하는 것을 먼저 만들자"는 판단으로 캐싱을 후순위로 미뤘다.

#### 구현 중 발견된 문제

Google Places API (New)는 두 단계로 나뉜다.

1. **텍스트 검색** (`POST /v1/places:searchText`): 장소 기본 정보와 `photos[].name` 목록 반환
2. **사진 조회** (`GET /v1/{photoName}/media`): 실제 이미지 URL 반환

프론트엔드에서 장소 카드를 렌더링할 때 한 번의 장소 검색에 이어 사진 URL을 얻기 위해 사진 개수만큼 추가 API가 호출되는 구조였다. Google Places API도 월간 무료 크레딧 한도가 있기 때문에, 동일한 장소를 여러 사용자가 조회하는 상황에서 중복 호출이 반복되면 빠르게 쿼터가 소진될 수 있었다.

또한 사진 URL은 `photoName`(예: `places/ChIJ.../photos/Af...`)을 기준으로 구글이 발급하는 것이라 동일 `photoName`에 대한 URL은 일정 기간 변경되지 않는다. 매번 재요청할 이유가 없었다.

#### 해결: 검색과 사진 API를 분리하여 서로 다른 TTL 캐싱 적용

캐싱 대상과 TTL을 용도에 따라 분리했다.

```java
// 장소 검색: 1시간 캐싱 (검색 결과가 자주 바뀌지 않으나 실시간성도 어느 정도 보장)
@Cacheable(value = "placeSearch", key = "#text", unless = "#result == null")
public Optional<PlacesResponse> searchText(String text)

// 사진 URL: 24시간 캐싱 (URL 변경 빈도 낮음, API 호출 비용 절감 효과 큼)
@Cacheable(value = "placePhoto", key = "#photoName + '_' + #maxWidthPx", unless = "#result == null")
public Optional<PhotoMedia> getPhotoMedia(String photoName, int maxWidthPx)
```

`RedisCacheConfig`에서 각 캐시의 TTL과 직렬화 방식을 명시적으로 등록했다.

```java
"placeSearch" → POJO 직렬화, TTL 1시간
"placePhoto"  → POJO 직렬화, TTL 24시간
```

캐시 키에 `#text` 그대로를 사용하는 대신, `normalizeSearchText()`로 유니코드 공백 문자와 앞뒤 공백을 제거한 값을 실제 쿼리로 사용해 미묘하게 다른 입력이 서로 다른 캐시 키를 생성하는 현상도 방지했다.

---

### 1-4. 앨범 도메인: S3 Presigned URL → 서버 측 multipart 업로드 전환

#### 설계 단계의 접근

S3 파일 업로드 방식으로 설계 단계에서는 **Presigned URL** 방식을 선택했다. 클라이언트가 서버로부터 임시 URL을 받아 S3에 직접 업로드하는 방식으로, 서버 네트워크 부하를 줄일 수 있다는 이유였다.

```
클라이언트 → GET /api/album/presigned-url → 서버가 S3 Presigned URL 발급
클라이언트 → PUT {presignedUrl} + 파일 바이트 → S3 직접 업로드
클라이언트 → POST /api/album/photos (s3Key) → 서버 DB 저장
```

#### 구현 중 발견된 문제

실제 구현하면서 다음 문제가 드러났다.

1. **클라이언트 복잡도 증가**: URL 발급 → 직접 업로드 → DB 저장의 3단계 흐름을 프론트엔드가 모두 처리해야 했다. 업로드 중 실패 시 중간 상태(S3에는 올라갔으나 DB 미저장) 처리가 복잡했다.
2. **파일 유효성 검증 불가**: 서버를 경유하지 않으므로 파일 타입(jpeg/png/webp/heic), 파일 크기(10MB), 요청당 파일 수(최대 20개) 검증을 서버에서 사전에 처리할 수 없었다.
3. **트랜잭션 정합성**: S3 업로드와 DB INSERT가 별도 요청으로 분리되어 원자성 보장이 어려웠다.

#### 해결: 서버 측 multipart/form-data 업로드로 전환

```
클라이언트 → POST /api/album/{albumId}/photos (multipart/form-data)
                └─ 서버: 파일 검증 → S3Service.uploadFile() → DB INSERT (단일 트랜잭션)
```

서버가 파일을 받아 검증 후 S3 업로드와 DB 저장을 하나의 트랜잭션 내에서 처리하는 방식으로 변경했다. 동시에 `AlbumPhotoController`를 `AlbumController`(조회)와 분리하고, `S3Service.generatePresignedUploadUrl()` 메서드를 제거하여 불필요한 코드를 정리했다.

---

### 1-5. 앨범/TravelLog JPA 쿼리 성능 문제

#### 앨범 N+1 문제

앨범 사진 목록 조회 시 기본 Lazy 로딩으로 인해 사진마다 업로더(`uploadedBy`) 정보를 별도 쿼리로 조회하는 N+1이 발생했다. `JOIN FETCH` 쿼리 메서드를 추가하고, 삭제 시에도 `findByIdWithUploader()`로 단일 쿼리에서 업로더 정보를 함께 로드하도록 수정했다.

#### TravelLog Cartesian Product 문제

TravelLog 목록 조회에서 `photos`를 `LEFT JOIN FETCH`로 함께 불러오는 방식을 사용했는데, 로그 한 건에 사진이 여러 개 붙으면 **Cartesian Product**가 발생하여 row 수가 사진 수만큼 뻥튀기되는 문제가 있었다.

목록 조회에서 실제로 필요한 것은 사진의 전체 목록이 아니라 `photoCount`뿐이었다. JPQL constructor projection과 `SIZE()` 서브쿼리를 사용하는 방식으로 교체하여 불필요한 JOIN을 제거했다.

```java
// 변경 전: LEFT JOIN FETCH photos → 사진 수만큼 row 중복
// 변경 후: JPQL constructor projection
@Query("""
    SELECT new com.sofly.core.domain.travellog.dto.TravellogSummaryResponse(
        t.id, t.title, t.visibility, t.createdAt, SIZE(t.photos))
    FROM TravelLog t WHERE t.workspace.id = :workspaceId
""")
```

---

### 1-6. 항공편 출발 시간 Timezone 처리 버그

#### 발생 상황

Kafka Consumer(`ConquestConsumer`)에서 항공편 출발 시각이 현재보다 이전인지 비교하는 로직이 있었다. 초기 구현에서 `departureTime`을 `LocalDateTime`으로 처리했는데, KST(+09:00)로 입력된 시각을 UTC 기준 `LocalDateTime.now()`와 비교하면서 9시간 오차가 발생했다.

예를 들어 출발 시각이 `2026-05-01 09:00 KST`인 항공편이 실제로는 `2026-05-01 00:00 UTC`에 출발하는 것인데, 서버가 UTC 기준 `LocalDateTime.now()`와 비교하면 아직 출발하지 않은 것으로 판단하는 케이스가 생겼다.

#### 해결

`departureTime` 타입을 `LocalDateTime` → `ZonedDateTime`으로 변경하여 타임존 정보를 명시적으로 포함했다. Kafka DTO(`FlightSavedMessage`), 요청 DTO(`SaveFlightRequest`), `ConquestConsumer` 비교 로직 모두 `ZonedDateTime.toInstant()`과 `Instant.now()` 기준으로 통일했다.

```java
// 변경 전: 타임존 없는 비교
if (departureTime.isBefore(LocalDateTime.now(ZoneOffset.UTC)))

// 변경 후: Instant 기준 명시적 비교
if (message.getDepartureTime().toInstant().isBefore(Instant.now()))
```

---

### 1-7. AI 채팅 — Stream.toList() 불변 리스트 런타임 오류

#### 발생 상황

Spring AI의 `RdbChatMemory`에서 DB에서 조회한 메시지 목록을 `Stream.toList()`로 반환했다. Java 16+의 `Stream.toList()`는 **불변 리스트**를 반환하는데, Spring AI 내부의 `MessageAggregator`가 이 리스트에 `.add()`를 호출하면서 `UnsupportedOperationException`이 런타임에 발생했다.

```
UnsupportedOperationException
  at java.base/java.util.ImmutableCollections.umodifiableCheck
  at MessageAggregator.add(...)
```

설계 단계에서는 Spring AI 내부 구현 상세를 파악하지 못해 이 문제를 예측하지 못했다.

#### 해결

`Stream.toList()` 대신 `Collectors.toList()`로 변경하여 수정 가능한(mutable) `ArrayList`가 반환되도록 했다.

```java
// 변경 전
return messages.stream()
    .map(this::toSpringAiMessage)
    .toList();  // 불변 리스트

// 변경 후
return messages.stream()
    .map(this::toSpringAiMessage)
    .collect(Collectors.toList());  // mutable ArrayList
```

---

### 1-8. 공급사(Supplier) 전환: Amadeus → Booking.com (RapidAPI)

#### 설계 단계의 접근

항공편/호텔 검색의 기본 공급사로 설계 단계에서는 **Amadeus API**를 선택했다. 항공 업계 표준 API로 데이터 품질이 높고, 공식 Java SDK가 제공된다는 이유였다.

#### 구현 중 발견된 문제

1. **인증 복잡성**: Amadeus는 OAuth2 Client Credentials 방식으로 Access Token을 별도로 발급받아야 했다. Token TTL(30분)을 관리하는 `AmadeusAuthClient`와 `TokenCache`를 별도로 구현해야 했으며, 토큰 만료 시 재발급 로직이 API 호출과 결합되어 코드가 복잡해졌다.
2. **응답 포맷 파싱 난이도**: Amadeus 응답 스키마가 GDS 기반 항공 업계 포맷(IATA 코드, 복잡한 중첩 구조)이라 파싱 코드 작성 비용이 높았다.
3. **무료 티어 제한**: Amadeus 테스트 환경의 데이터가 실제 운항 스케줄과 다른 테스트 데이터 위주라 프론트엔드 연동 시 사용성 검증이 어려웠다.

#### 해결: Booking.com RapidAPI로 기본 공급사 전환

RapidAPI를 통한 Booking.com API는 단순히 `X-RapidAPI-Key` 헤더 하나로 인증이 끝나고, 응답이 JSON 평문 구조라 파싱이 훨씬 단순했다. `SupplierRegistry`의 헥사고날 아키텍처 덕분에 `FlightSupplierPort` 구현체를 교체하는 것만으로 기본 공급사 변경이 완료되었다.

Amadeus 관련 파일(AmadeusAuthClient, AmadeusProperties, AmadeusTokenResponse, TokenCache 등)은 전량 삭제하여 코드베이스를 정리했다.

---

### 1-9. WebSocket 채팅 — STOMP JWT 인증 + Redis Pub/Sub 브로드캐스트

#### 발생 상황

WebSocket STOMP 채팅 구현 초기에 두 가지 문제가 있었다.

**문제 1: WebSocket 연결 시 인증 처리 누락**  
HTTP 기반 REST API는 `JwtAuthenticationFilter`가 처리하지만, WebSocket STOMP 연결(`CONNECT` 프레임)은 HTTP 핸드셰이크 이후 별도 프로토콜로 동작하여 기존 JWT 필터가 적용되지 않았다. 인증 없이 아무나 채팅방에 메시지를 보낼 수 있는 상태였다.

**문제 2: SimpMessagingTemplate 단일 인스턴스 브로드캐스트 한계**  
단일 서버에서는 `SimpMessagingTemplate`으로 브로드캐스트가 동작하지만, blue-green 배포 환경에서 인스턴스가 두 개 이상 올라갈 경우 메시지를 발행한 인스턴스에 연결된 사용자에게만 전달되는 문제가 예상되었다.

#### 해결

**STOMP 인증**: `StompChannelInterceptor`를 구현하여 `CONNECT` 프레임 수신 시 STOMP 헤더에서 JWT를 추출하고 검증한 뒤, 인증된 사용자 정보(userId, nickname)를 STOMP 세션에 저장하도록 했다. 이후 `SEND` 프레임에서는 세션에 저장된 사용자 정보를 활용하여 요청 DTO에 senderId를 포함하지 않아도 서버 측에서 신원을 확인할 수 있게 했다.

**Redis Pub/Sub 브로드캐스트**: 메시지 발행 시 `SimpMessagingTemplate` 직접 호출 대신 Redis `ChannelTopic`에 publish하고, `RedisSubscriber`가 구독하여 `SimpMessagingTemplate`으로 전달하는 구조로 변경했다. 모든 인스턴스가 동일 Redis Pub/Sub 채널을 구독하므로 어떤 인스턴스에 연결된 사용자든 메시지를 수신할 수 있다.

---

## 2. 설계 변경 근거 (ADR 이력)

### ADR-01. 정복지도 상태 전환 아키텍처 변경

| 항목 | 내용 |
|------|------|
| **결정 일자** | 2026년 5월 (구현 단계) |
| **변경 전** | Spring 내부 이벤트(`ApplicationEventPublisher`) + DB 풀스캔 스케줄러(30분) |
| **변경 후** | Kafka 비동기 이벤트 + Redis ZSet 기반 스케줄러(1분) |
| **변경 이유** | DB 풀스캔의 확장성 한계, Spring 내부 이벤트의 동기 처리로 인한 장애 전파 위험, 후속 기능(알림, 일정 자동 구성) 확장 시 결합도 증가 문제 |
| **트레이드오프** | Kafka 인프라 추가 운영 비용 발생. 단일 노드 KRaft 모드로 운영하여 최소화 |

### ADR-02. 항공편 목적지 검색 캐싱 전략 세분화

| 항목 | 내용 |
|------|------|
| **결정 일자** | 2026년 5월 (구현 단계) |
| **변경 전** | 모든 쿼리 문자열을 조건 없이 캐싱 |
| **변경 후** | 2글자 미만 쿼리는 API 호출 및 캐싱 차단, 빈 결과는 캐싱 제외 |
| **변경 이유** | 1글자 쿼리로 인한 불필요한 RapidAPI 호출 및 Redis 낭비 발견 |
| **트레이드오프** | 1글자 입력 시 즉각적인 자동완성 결과가 없음. 실용적으로 1글자 공항 코드 검색 케이스가 없어 허용 가능한 UX 트레이드오프로 판단 |

### ADR-04. 앨범 S3 업로드 방식 변경 (Presigned URL → 서버 측 multipart)

| 항목 | 내용 |
|------|------|
| **결정 일자** | 2026년 4월 (구현 단계) |
| **변경 전** | 클라이언트가 Presigned URL 발급 → S3 직접 업로드 → 서버 DB 저장 |
| **변경 후** | 서버가 multipart 수신 → 파일 검증 → S3 업로드 + DB 저장 (단일 트랜잭션) |
| **변경 이유** | 3단계 흐름의 프론트엔드 복잡도, 서버 측 파일 유효성 검증 불가, 업로드 중단 시 S3/DB 불일치 위험 |
| **트레이드오프** | 파일이 서버를 경유하므로 서버 네트워크 트래픽 증가. 10MB × 20개 = 최대 200MB/요청. 실제 트래픽 규모 상 허용 가능한 수준으로 판단 |

### ADR-05. 기본 항공편/호텔 공급사 변경 (Amadeus → Booking.com RapidAPI)

| 항목 | 내용 |
|------|------|
| **결정 일자** | 2026년 4월 (구현 단계) |
| **변경 전** | Amadeus API (기본 공급사) |
| **변경 후** | Booking.com via RapidAPI (기본 공급사 `booking`) |
| **변경 이유** | Amadeus OAuth2 Token 관리 복잡성, GDS 기반 복잡한 응답 파싱 비용, 테스트 환경 데이터 품질 문제 |
| **트레이드오프** | RapidAPI 월간 쿼터 의존도 증가. 헥사고날 아키텍처(`SupplierRegistry`)로 미래에 다시 교체 가능한 구조 유지 |

### ADR-03. Google Places API 캐싱 추가

| 항목 | 내용 |
|------|------|
| **결정 일자** | 2026년 5월 (구현 단계) |
| **변경 전** | 캐싱 미적용, 매 요청마다 외부 API 호출 |
| **변경 후** | 장소 검색 1시간 / 사진 URL 24시간 Redis 캐싱 |
| **변경 이유** | 동일 장소 반복 조회 시 API 크레딧 소진 속도가 예상보다 빠름을 실측. 사진 API는 텍스트 검색 결과 한 건당 여러 번 호출되는 구조라 영향이 더 컸음 |
| **트레이드오프** | 장소 정보가 1시간 내에 변경되더라도 캐시된 이전 정보가 반환될 수 있음. 개업/폐업 같은 급격한 변화는 반영이 지연될 수 있으나, 여행 계획 서비스 특성상 수용 가능한 수준으로 판단 |

---

## 3. 프로젝트 수행 계획 대비 실행 현황

### 3-1. 전체 현황 요약

| 기능 영역 | 계획 | 현황 |
|----------|------|------|
| 인증 (Google/Kakao/Naver OAuth2 + JWT) | 완료 | 완료 |
| 워크스페이스 / 멤버 관리 | 완료 | 완료 |
| 항공편 저장 및 연동 | 완료 | 완료 |
| 정복지도 (Redis ZSet + Kafka) | 완료 | 완료 |
| AI 여행 플래너 채팅 (Gemini) | 완료 | 완료 |
| 일정 관리 (Schedule/ScheduleItem) | 완료 | 완료 |
| 앨범 (AWS S3) | 완료 | 완료 |
| 여행 로그 (TravelLog) | 완료 | 완료 |
| 항공편/호텔 검색 (Booking.com RapidAPI) | 완료 | 완료 |
| Google Places 장소 검색 및 캐싱 | 완료 | 완료 |
| 알림 발송 (FCM/WebSocket) | 미완 | 인프라 구현 전 Stub 상태 |
| 모니터링 스택 (Prometheus/Grafana/Zipkin) | 추가 | 설계 단계 이후 추가 구성 완료 |
| SNS 기능 (팔로우/좋아요/댓글) | 진행 중 | Phase 1 데이터 레이어 완료, API 레이어 구현 중 |
| **성능 부하 테스트** | **계획** | **미착수 (하기 기술)** |

---

### 3-2. 지연 기능: 성능 부하 테스트

#### 현황

성능 부하 테스트는 수행 계획에 포함되어 있었으나 현재 미착수 상태다.

#### 지연 원인

**1. 테스트 환경 구성의 모호함**

JMeter, Gatling, k6 등 부하 테스트 도구에 대한 팀 내 경험이 없어 러닝 커브가 발생했다. 단순히 도구를 익히는 것 외에도 다음 질문들에 대한 답을 정하지 못한 상태다.

- 어떤 시나리오를 테스트할 것인가 (최대 동시 사용자 수? 특정 API 응답 시간? Redis 캐시 히트율?)
- 테스트 환경을 로컬에 구성할 것인가, 스테이징 서버에서 진행할 것인가
- 측정 지표를 어떻게 수집하고 해석할 것인가 (Prometheus + Grafana 연동 여부)

**2. 외부 API 사용량 제한**

부하 테스트에서 핵심적으로 검증하고 싶은 시나리오는 항공편 검색(Booking.com RapidAPI)과 Google Places 장소 검색이다. 그런데 이 두 API는 모두 월간 호출 쿼터가 존재한다.

- **RapidAPI (Booking.com)**: 무료 플랜 기준 월간 호출 수 상한이 있으며, 부하 테스트 수준의 대량 반복 호출을 실행하면 쿼터를 단기간에 소진할 위험이 있다.
- **Google Places API**: 월 200달러 무료 크레딧이 제공되지만, `searchText` 호출 단가가 높아 부하 테스트 규모에 따라 비용이 빠르게 발생할 수 있다.

이런 특성 때문에 단순히 부하를 높여보는 방식으로는 테스트를 진행하기 어렵다. 실제 외부 API 호출 없이 Redis 캐시 히트 경로만 테스트하는 시나리오와, 제한된 호출 수 내에서 Cache Miss 경로를 측정하는 시나리오를 분리해서 설계해야 한다는 결론은 냈지만, 구체적인 환경 구성까지 진행하지 못한 상태다.

#### 계획 중인 접근 방향

| 단계 | 방법 | 목적 |
|------|------|------|
| 1단계 | Redis 캐시 Pre-warming 후 부하 테스트 | 외부 API 호출 없이 캐시 히트 경로 응답 시간 측정 |
| 2단계 | WireMock으로 외부 API Mock | RapidAPI/Google Places 를 Mock 서버로 대체하여 쿼터 소진 없이 Cache Miss 경로 테스트 |
| 3단계 | 실제 API 소규모 호출 | 단계별 점진적 부하 (10 → 50 → 100 VU) 로 쿼터 소진을 제어하며 실측 |

현재 팀 일정상 1단계부터 순서대로 진행할 예정이며, k6를 도구로 검토 중이다.

---

### 3-3. 계획 외 추가 구현: 모니터링 스택

서비스 운영 중 컨테이너 상태와 캐시 동작을 실시간으로 파악할 수단이 없어 디버깅이 어려웠다. 이 문제를 해결하기 위해 계획에 없었던 모니터링 스택을 별도 `docker-compose.monitoring.yml`로 구성했다.

| 도구 | 역할 |
|------|------|
| Prometheus | core/supply 서비스 메트릭 수집 (`/actuator/prometheus` 스크레이핑) |
| Grafana | 메트릭 시각화 대시보드 |
| cAdvisor | 컨테이너별 CPU/메모리 사용량 모니터링 |
| Redis Exporter | Redis 커넥션 수, 히트율, 메모리 사용량 시각화 |
| Zipkin | 분산 추적 (core ↔ supply 서비스 간 요청 흐름, WebClient 호출 지연 측정) |
| Dozzle | 도커 컨테이너 로그 실시간 조회 (Nginx 역방향 프록시 경유) |

HikariCP 커넥션 풀 설정 및 모니터링도 이 단계에서 추가했다. cAdvisor의 CPU 과사용 문제는 `housekeeping interval`을 15초로 조정하여 해결했다.

---

### 3-4. 리소스 재분배 및 일정 조정

알림 발송(FCM/WebSocket) 기능과 WorkspaceConsumer(항공편 기반 일정 자동 구성)는 스펙 확정이 늦어져 현재 Stub 상태다. 해당 공수를 줄이고 확보된 백엔드 리소스를 정복지도 아키텍처 개선(ADR-01), 캐싱 전략 고도화(ADR-02~05), 모니터링 스택 구성에 집중 투입했다. 설계 단계에서 예상하지 못한 N+1, Cartesian Product, 타임존 버그 등의 수정도 이 기간에 처리하여 핵심 기능의 품질과 안정성을 높이는 방향으로 일정을 재조정했다.

---

## 참고 문서

- [Kafka 아키텍처 가이드](../docs/kafka-architecture.md)
- [Redis 캐싱 전략 — travel-supply-service](../docs/redis-caching.md)
- [Conquest Map 기술 문서](../services/travel-core-service/docs/conquest-map.md)
- [Kafka + Redis ZSet 전환 구현 계획](../services/travel-core-service/docs/feat-conquest-map-kafka-flight-departure.md)

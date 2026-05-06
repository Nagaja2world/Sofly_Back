# Redis 캐싱 전략 — travel-supply-service

## 개요

`travel-supply-service`는 Booking.com RapidAPI 호출 비용을 줄이고 응답 속도를 높이기 위해 Spring Cache + Redis를 사용한다.  
설정은 `RedisCacheConfig`에 집중되어 있으며, 각 어댑터 메서드에 `@Cacheable`을 선언하는 방식으로 적용된다.

---

## 캐시 설정 (`RedisCacheConfig`)

```
config/RedisCacheConfig.java
```

### 직렬화 방식

| 방식 | 클래스 | 사용처 |
|------|--------|--------|
| `Jackson2JsonRedisSerializer<JsonNode>` | `jsonNodeCacheConfig` | 검색 결과 / 필터 (JsonNode 반환 메서드) |
| `GenericJackson2JsonRedisSerializer` | `pojoCacheConfig` | 목적지·정렬 등 POJO 리스트 |

키는 모든 캐시에서 `StringRedisSerializer`로 문자열 직렬화된다.

### 캐시별 TTL 요약

| 캐시 이름 | TTL | 직렬화 | 담당 클래스 |
|-----------|-----|--------|------------|
| `bookingFlights` | 5분 (default) | JsonNode | `BookingComFlightSupplierAdapter` |
| `bookingFlightDetails` | 5분 (default) | JsonNode | `BookingComFlightSupplierAdapter` |
| `bookingHotels` | 5분 (default) | JsonNode | `BookingComHotelAdapter` |
| `flightDestinations` | 24시간 | POJO | `BookingComFlightMetaClient` |
| `hotelDestinations` | 24시간 | POJO | `BookingComHotelMetaClient` |
| `hotelSortBy` | 24시간 | POJO | `BookingComHotelMetaClient` |
| `hotelFilter` | 24시간 | JsonNode | `BookingComHotelMetaClient` |

> `bookingFlights` / `bookingFlightDetails` / `bookingHotels`는 `withInitialCacheConfigurations`에 명시되지 않아 `cacheDefaults`(5분 JsonNode)가 적용된다.

---

## 캐시 적용 상세

### 항공편 검색 (`bookingFlights`)

```java
// BookingComFlightSupplierAdapter.java
@Cacheable(value = "bookingFlights", key = "#request")
public JsonNode searchFlightOffers(FlightSearchRequest request)
```

- **키**: `FlightSearchRequest.toString()` — `@ToString`이 모든 필드를 포함하므로 요청 파라미터 전체가 키가 된다.
- **주의**: `airlines` 필터가 있으면 내부에서 페이지를 반복 호출(`fetchFilteredUntilFull`)하지만, 캐시는 최종 조합 결과를 `#request` 단위로 저장한다. `cursor` 필드가 다르면 별도 캐시 엔트리가 생성된다.

### 항공편 상세 (`bookingFlightDetails`)

```java
@Cacheable(value = "bookingFlightDetails", key = "#token + ':' + #currencyCode")
public JsonNode getFlightDetails(String token, String currencyCode)
```

- **키**: `"<token>:<currencyCode>"` 형태의 문자열 조합.
- 동일 토큰 + 통화 조합은 5분 내 재요청 시 캐시에서 반환된다.

### 호텔 검색 (`bookingHotels`)

```java
// BookingComHotelAdapter.java
@Cacheable(value = "bookingHotels", key = "#request")
public JsonNode searchHotelsByCity(HotelSearchRequest request)
```

- **키**: `HotelSearchRequest.toString()`.
- `HotelSearchRequest`는 `@EqualsAndHashCode` + `@ToString`이 선언되어 있어 모든 필드가 키 생성에 포함된다.

### 항공 목적지 메타 (`flightDestinations`)

```java
// BookingComFlightMetaClient.java
@Cacheable(value = "flightDestinations", key = "#query + ':' + #languageCode")
public List<FlightDestination> searchDestinations(String query, String languageCode)
```

- **키**: `"<query>:<languageCode>"`.
- 공항/도시 코드 목록은 자주 바뀌지 않으므로 24시간 캐싱.

### 호텔 목적지 메타 (`hotelDestinations`)

```java
// BookingComHotelMetaClient.java
@Cacheable(value = "hotelDestinations", key = "#query")
public List<HotelDestination> searchDestination(String query)
```

- **키**: `query` 문자열 그대로.

### 호텔 정렬 옵션 (`hotelSortBy`)

```java
@Cacheable(value = "hotelSortBy", key = "#request")
public List<HotelSortOption> getSortBy(HotelOptionsRequest request)
```

- **키**: `HotelOptionsRequest.toString()`.

### 호텔 필터 옵션 (`hotelFilter`)

```java
@Cacheable(value = "hotelFilter", key = "#request")
public JsonNode getFilter(HotelOptionsRequest request)
```

- **키**: `HotelOptionsRequest.toString()`.
- `JsonNode` 반환이므로 `jsonNodeMetaConfig`(JsonNode 직렬화, 24시간)가 적용된다.

---

## 캐시 키 설계 원칙

모든 캐시 키는 Spring Cache SpEL 표현식으로 정의된다.

| 패턴 | 예시 | 사용 이유 |
|------|------|-----------|
| `#request` (DTO toString) | `bookingHotels`, `bookingFlights` | 요청 파라미터 전체를 키로 사용, 동일 조건 재요청 히트 |
| 필드 직접 조합 | `#query + ':' + #languageCode` | 단순 파라미터가 2개 이하일 때 명시적 조합 |
| 단일 파라미터 | `#query`, `#token + ':' + #currencyCode` | 파라미터가 하나 또는 단순 조합일 때 |

> DTO를 캐시 키로 쓰려면 `@EqualsAndHashCode`와 `@ToString`이 반드시 필요하다. `HotelSearchRequest`, `HotelOptionsRequest`, `FlightSearchRequest` 모두 선언되어 있다.

---

## Redis 연결 설정

```yaml
# application.yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

환경변수 `REDIS_HOST` / `REDIS_PORT`로 주입하며, 미설정 시 로컬 기본값(`localhost:6379`)을 사용한다.

---

## TTL 결정 근거

| 구분 | TTL | 이유 |
|------|-----|------|
| 검색 결과 (항공/호텔) | 5분 | 실시간 가격·잔여석 변동 반영, 단기 중복 요청 흡수 |
| 메타 데이터 (목적지·정렬·필터) | 24시간 | 변경 빈도 낮음, API 쿼터 절약 |

---

## 현재 캐싱이 없는 영역

- **항공편 필터링 중간 페이지 fetch** (`fetchPage` 내부): `@Cacheable` 없이 매번 외부 API 호출.  
  항공사 필터가 있을 때 최대 10페이지를 순회하는 로직이라 캐시 히트율이 낮아 의도적으로 제외된 것으로 보인다.
- **Google Places**: `GooglePlacesClient`는 캐시 미적용. 이미지 URL은 요청 시마다 Google Places API를 호출한다.

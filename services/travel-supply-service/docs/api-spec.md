# API Spec — Travel Supply Service

Base URL: `http://localhost:8082`

---

## Endpoints

### GET /supply/flights/offers

항공편 검색. Amadeus `/v2/shopping/flight-offers` 응답을 그대로 반환한다.

**Query Parameters**

| 파라미터 | 필수 | 기본값 | 설명 |
|---------|------|--------|------|
| `origin` | required | — | 출발 공항 IATA 코드 (예: `ICN`) |
| `dest` | required | — | 도착 공항 IATA 코드 (예: `NRT`) |
| `date` | required | — | 출발일 (`yyyy-MM-dd`) |
| `adults` | optional | `1` | 성인 인원 수 |
| `max` | optional | `5` | 최대 결과 수 |
| `supplier` | optional | 설정값(`amadeus`) | 사용할 공급사 키 |

**Response**

Amadeus raw JSON (`JsonNode`) — `data` 배열에 항공편 오퍼 목록 포함.

---

### GET /supply/hotels/offers

호텔 검색. 도시 코드로 최대 20개 호텔의 오퍼를 조회한다.

**Query Parameters**

| 파라미터 | 필수 | 기본값 | 설명 |
|---------|------|--------|------|
| `cityCode` | required | — | 도시 IATA 코드 (예: `PAR`, `TYO`) |
| `checkIn` | required | — | 체크인 날짜 (`yyyy-MM-dd`) |
| `checkOut` | required | — | 체크아웃 날짜 (`yyyy-MM-dd`) |
| `adults` | optional | `1` | 성인 인원 수 |
| `roomQuantity` | optional | `1` | 객실 수 |

**Response** — `List<HotelSearchResult>`

```json
[
  {
    "hotelId": "ADPAR001",
    "hotelName": "Hotel Le Marais",
    "cityCode": "PAR",
    "latitude": 48.8566,
    "longitude": 2.3522,
    "offers": { }
  }
]
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `hotelId` | `String` | Amadeus 호텔 ID |
| `hotelName` | `String` | 호텔명 |
| `cityCode` | `String` | 도시 IATA 코드 |
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `offers` | `JsonNode` | Amadeus 오퍼 원본 데이터 (가격, 객실 정보 등) |

---

### GET /supply/hotels/place-info

호텔명 + 도시 코드로 Google Places 정보를 조회한다.

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `hotelName` | required | 호텔명 (예: `Hyatt Regency`) |
| `cityCode` | required | 도시 IATA 코드 (예: `PAR`) |

**Response** — `PlaceInfo`

```json
{
  "placeId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
  "rating": 4.3,
  "userRatingsTotal": 1024
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `placeId` | `String` | Google Place ID |
| `rating` | `Double` | 평점 (0.0 ~ 5.0) |
| `userRatingsTotal` | `Integer` | 리뷰 수 |

`GOOGLE_PLACES_API_KEY` 미설정 시 `null` 반환.

---

### GET /supply/places _(dev/test only)_

> `prod` 프로파일에서는 비활성화됨 (`@Profile("!prod")`)

Google Places를 직접 조회하는 테스트 엔드포인트.

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `hotelName` | required | 호텔명 |
| `cityCode` | required | 도시 IATA 코드 |

**Response** — `PlaceInfo` (위와 동일)

---

## Schemas

### HotelSearchResult

```
hotelId         String   Amadeus 호텔 ID
hotelName       String   호텔명
cityCode        String   도시 IATA 코드
latitude        Double   위도
longitude       Double   경도
offers          JsonNode Amadeus 오퍼 원본 (가격, 객실 등)
```

### PlaceInfo

```
placeId          String  Google Place ID
rating           Double  평점 (0.0 ~ 5.0)
userRatingsTotal Integer 리뷰 수
```

---

## Enums / 코드 규칙

| 종류 | 형식 | 예시 |
|------|------|------|
| 공항/도시 코드 | IATA 3자리 대문자 | `ICN`, `NRT`, `PAR`, `TYO` |
| 날짜 | ISO 8601 (`yyyy-MM-dd`) | `2026-05-01` |
| supplier 키 | 소문자 문자열 | `amadeus` |

---

## 에러 케이스

| 상황 | 동작 |
|------|------|
| 해당 도시에 호텔 없음 | `500` — `No hotels found for city: {cityCode}` |
| 호텔 ID 목록이 비어있음 | `500` — `No hotel IDs found for city: {cityCode}` |
| Google Places API 키 미설정 | `null` 반환 (예외 없음) |
| Amadeus 인증 실패 | WebClient 예외 전파 |

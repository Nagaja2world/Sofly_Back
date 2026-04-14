# API Spec — Travel Supply Service

Base URL: `http://localhost:8081`  
Swagger UI: `http://localhost:8081/supply/swagger-ui`

공급사(supplier): 현재 **booking** 단일 공급사 사용 (`app.suppliers.default: booking`)

---

## Endpoints

### Flight

#### GET /supply/flights/offers

항공편 검색. Booking.com RapidAP/ raw JSON을 반환한다.

**Query Parameters**

| 파라미터 | 필수 | 기본값 | 설명 |
|---------|------|--------|------|
| `supplier` | optional | `booking` | 공급사 키 |
| `fromId` | required | `ICN.AIRPORT` | 출발지 ID (예: `ICN.AIRPORT`) |
| `toId` | required | `CEB.AIRPORT` | 도착지 ID (예: `CEB.AIRPORT`) |
| `departDate` | required | `2026-04-04` | 출발일 (`yyyy-MM-dd`) |
| `returnDate` | optional | `2026-04-06` | 귀국일 (`yyyy-MM-dd`, 편도 시 생략) |
| `adults` | optional | `2` | 성인 인원 수 |
| `currencyCode` | optional | `KRW` | 통화 코드 |
| `stops` | optional | `none` | 경유 횟수 (`none` \| `ZERO` \| `ONE` \| `TWO`) |
| `sort` | optional | `BEST` | 정렬 (`BEST` \| `CHEAPEST` \| `FASTEST`) |
| `cabinClass` | optional | `ECONOMY` | 좌석 등급 (`ECONOMY` \| `PREMIUM_ECONOMY` \| `BUSINESS` \| `FIRST`) |
| `childrenAge` | optional | — | 어린이 나이 (쉼표 구분, 예: `5,7`) |
| `pageNo` | optional | `1` | 페이지 번호 |

**Response** — Booking.com raw JSON (`JsonNode`)

---

#### GET /supply/flights/destinations

공항/도시 검색. 항공편 검색 전 `fromId`/`toId` 값을 얻기 위해 사용한다.

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `query` | required | 검색어 (공항명, 도시명, 국가명 등, 예: `korea`) |
| `languageCode` | required | 언어 코드 (예: `en-us`, `ko`) |

**Response** — `List<FlightDestination>`

```json
[
  {
    "id": "ICN.AIRPORT",
    "type": "AIRPORT",
    "name": "Incheon International Airport",
    "code": "ICN",
    "city": "Seoul",
    "cityName": "Seoul",
    "regionName": "Seoul Capital Area",
    "country": "KR",
    "countryName": "South Korea",
    "countryNameShort": "South Korea",
    "photoUri": "https://...",
    "distanceToCity": { "value": 48.2, "unit": "km" },
    "parent": "seoul.ko"
  }
]
```

---

### Hotel

#### GET /supply/hotels/offers

호텔 검색. Booking.com RapidAPI raw JSON을 반환한다.  
목적지 `destId`/`searchType`은 `/supply/hotels/destinations`로 먼저 조회한다.

**Query Parameters**

| 파라미터 | 필수 | 기본값 | 설명 |
|---------|------|--------|------|
| `supplier` | optional | `booking` | 공급사 키 |
| `destId` | required | `267199` | 목적지 ID |
| `searchType` | required | `LANDMARK` | 검색 타입 (예: `CITY`, `LANDMARK`, `DISTRICT`) |
| `arrivalDate` | required | `2026-04-06` | 체크인 날짜 (`yyyy-MM-dd`) |
| `departureDate` | required | `2026-04-08` | 체크아웃 날짜 (`yyyy-MM-dd`) |
| `adults` | optional | `1` | 성인 인원 수 |
| `childrenAge` | optional | `0` | 어린이 나이 (쉼표 구분) |
| `roomQty` | optional | `1` | 객실 수 |
| `pageNumber` | optional | `1` | 페이지 번호 |
| `priceMin` | optional | `0` | 최소 가격 |
| `priceMax` | optional | `0` | 최대 가격 (0이면 제한 없음) |
| `sortBy` | optional | `price` | 정렬 기준 |
| `categoriesFilter` | optional | — | 필터 카테고리 (`/supply/hotels/filter-options`에서 조회) |
| `units` | optional | `METRIC` | 단위 (`METRIC` \| `IMPERIAL`) |
| `temperatureUnit` | optional | `Celsius` | 온도 단위 (`CELSIUS` \| `FAHRENHEIT`) |
| `languageCode` | optional | `ko` | 언어 코드 |
| `currencyCode` | optional | `KRW` | 통화 코드 |

**Response** — Booking.com raw JSON (`JsonNode`)

---

#### GET /supply/hotels/destinations

도시/지역명으로 `destId`와 `searchType`을 조회한다. 호텔 검색 전 먼저 호출한다.

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `query` | required | 검색어 (예: `seoul`) |

**Response** — `List<HotelDestination>`

```json
[
  {
    "destId": "738144",
    "destType": "CITY",
    "name": "Seoul",
    "label": "Seoul, Seoul, South Korea",
    "cityName": "Seoul",
    "country": "South Korea",
    "region": null,
    "latitude": 37.5665,
    "longitude": 126.978,
    "imageUrl": "https://...",
    "hotels": 2154
  }
]
```

---

#### GET /supply/hotels/sort-options

호텔 검색에 사용 가능한 `sortBy` 값 목록을 조회한다.

**Query Parameters** — `HotelOptionsRequest` (`destId`, `searchType`, `arrivalDate`, `departureDate` 필수)

**Response** — `List<HotelSortOption>`

```json
[
  { "id": "price", "title": "가격 낮은 순" },
  { "id": "review_score", "title": "평점 높은 순" }
]
```

---

#### GET /supply/hotels/filter-options

호텔 검색에 사용 가능한 `categoriesFilter` 값 목록을 조회한다.  
초기 요청 시 `categoriesFilter`는 비워두면 된다.

**Query Parameters** — `HotelOptionsRequest` 동일

**Response** — Booking.com raw JSON (`JsonNode`)

---

### Google Places _(dev/test only — `@Profile("!prod")`)_

#### GET /supply/places

텍스트로 Google Places를 검색한다.

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `text` | required | 검색 텍스트 (예: `Lotte Hotel Seoul`) |

**Response** — `PlacesResponse`

```json
{
  "places": [
    {
      "id": "ChIJ92BD_-2ifDURWdfj5R8TWjs",
      "displayName": { "text": "롯데호텔서울", "languageCode": "ko" },
      "primaryType": "hotel",
      "formattedAddress": "서울특별시 중구 을지로 30",
      "location": { "latitude": 37.5652, "longitude": 126.9814 },
      "rating": 4.4,
      "userRatingCount": 3821,
      "editorialSummary": { "text": "럭셔리 호텔...", "languageCode": "ko" },
      "websiteUri": "https://www.lottehotel.com",
      "nationalPhoneNumber": "02-771-1000",
      "googleMapsUri": "https://maps.google.com/?cid=...",
      "businessStatus": "OPERATIONAL",
      "priceLevel": "PRICE_LEVEL_VERY_EXPENSIVE",
      "photos": [
        {
          "name": "places/ChIJ.../photos/AXCi...",
          "widthPx": 4032,
          "heightPx": 3024
        }
      ]
    }
  ]
}
```

---

#### GET /supply/places/photo

`places:searchText` 응답의 `photos[].name`으로 실제 이미지 URL을 조회한다.  
photo name은 임시 참조값이므로 매번 searchText → photo 순서로 호출해야 한다.

**Query Parameters**

| 파라미터 | 필수 | 기본값 | 설명 |
|---------|------|--------|------|
| `name` | required | — | photo name (예: `places/ChIJ.../photos/AXCi...`) |
| `maxWidthPx` | optional | `800` | 이미지 최대 너비 (px) |

**Response** — `PhotoMedia`

```json
{
  "name": "places/ChIJ.../photos/AXCi.../media",
  "photoUri": "https://lh3.googleusercontent.com/..."
}
```

---

## Schemas

### FlightSearchRequest

| 필드 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `fromId` | `String` | `ICN.AIRPORT` | 출발지 ID |
| `toId` | `String` | `CEB.AIRPORT` | 도착지 ID |
| `departDate` | `LocalDate` | — | 출발일 |
| `returnDate` | `LocalDate` | — | 귀국일 (편도 시 생략) |
| `adults` | `int` | `2` | 성인 수 |
| `currencyCode` | `String` | `KRW` | 통화 |
| `stops` | `StopsType` | `none` | 경유 횟수 |
| `sort` | `SortType` | `BEST` | 정렬 |
| `cabinClass` | `CabinClass` | `ECONOMY` | 좌석 등급 |
| `childrenAge` | `String` | — | 어린이 나이 |
| `pageNo` | `Integer` | `1` | 페이지 |

### HotelDestination

| 필드 | 타입 | 설명 |
|------|------|------|
| `destId` | `String` | 목적지 ID (호텔 검색에 사용) |
| `destType` | `String` | 검색 타입 (`CITY`, `LANDMARK` 등) |
| `name` | `String` | 목적지명 |
| `label` | `String` | 표시용 전체 명칭 |
| `cityName` | `String` | 도시명 |
| `country` | `String` | 국가명 |
| `latitude` | `Double` | 위도 |
| `longitude` | `Double` | 경도 |
| `imageUrl` | `String` | 대표 이미지 URL |
| `hotels` | `Integer` | 검색 가능 호텔 수 |

### PlacesResponse.Place

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `String` | Google Place ID (영구값) |
| `displayName` | `DisplayName` | 장소명 (`text`, `languageCode`) |
| `primaryType` | `String` | 장소 유형 (예: `hotel`, `restaurant`) |
| `formattedAddress` | `String` | 주소 |
| `location` | `Location` | 좌표 (`latitude`, `longitude`) |
| `rating` | `Double` | 평점 (0.0 ~ 5.0) |
| `userRatingCount` | `Integer` | 리뷰 수 |
| `editorialSummary` | `LocalizedText` | 편집자 요약 (`text`, `languageCode`) |
| `websiteUri` | `String` | 웹사이트 URL |
| `nationalPhoneNumber` | `String` | 전화번호 |
| `googleMapsUri` | `String` | Google Maps 링크 |
| `businessStatus` | `String` | 영업 상태 (`OPERATIONAL` 등) |
| `priceLevel` | `String` | 가격대 (`PRICE_LEVEL_INEXPENSIVE` ~ `PRICE_LEVEL_VERY_EXPENSIVE`) |
| `photos` | `List<Photo>` | 사진 목록 (`name`, `widthPx`, `heightPx`) |

---

## Enums

### StopsType
| 값 | 설명 |
|----|------|
| `none` | 상관없음 |
| `ZERO` | 직항 |
| `ONE` | 1회 경유 |
| `TWO` | 2회 경유 |

### SortType
| 값 | 설명 |
|----|------|
| `BEST` | 최적 |
| `CHEAPEST` | 최저가 |
| `FASTEST` | 최단 시간 |

### CabinClass
| 값 | 설명 |
|----|------|
| `ECONOMY` | 이코노미 |
| `PREMIUM_ECONOMY` | 프리미엄 이코노미 |
| `BUSINESS` | 비즈니스 |
| `FIRST` | 퍼스트 |

---

## 에러 케이스

| 상황 | 동작 |
|------|------|
| 알 수 없는 supplier 키 | `500` — `Unknown flight/hotel supplier: {key}` |
| Google Places API 키 미설정 | `Optional.empty()` 반환 (예외 없음) |
| Google Places API 오류 | `Optional.empty()` 반환, 로그 출력 |

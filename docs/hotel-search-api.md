# 호텔 검색 API — 프론트엔드 연동 가이드

> **Base URL:** `http://localhost:8082` (로컬) / `https://api.sofly.com` (운영)  
> **서비스:** `travel-supply-service` (포트 8082)  
> **공통 prefix:** `/supply/hotels`

---

## UI 흐름과 API 매핑

```
목적지 입력창 자동완성  →  GET /supply/hotels/destinations
검색하기 버튼 클릭      →  GET /supply/hotels/offers
정렬 옵션 드롭다운 로드  →  GET /supply/hotels/sort-options
필터 패널 로드          →  GET /supply/hotels/filter-options
```

**권장 호출 순서:**
1. 목적지 입력 시 `destinations` 호출 → `destId`, `searchType` 획득
2. 검색 버튼 클릭 시 `sort-options` + `filter-options` + `offers` 동시 호출
3. 정렬/필터 변경 시 `offers` 재호출 (파라미터 변경)

---

## 1. 목적지 자동완성

> 목적지 입력창에 텍스트 입력 시 호출. 반환된 `destId`와 `searchType`을 이후 검색에 사용.

```
GET /supply/hotels/destinations?query={검색어}
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `query` | String | ✅ | 도시명, 지역명 등 | `"seoul"`, `"tokyo"` |

### Response

```json
[
  {
    "destId": "-716357",
    "destType": "CITY",
    "name": "Seoul",
    "label": "Seoul, Seoul, South Korea",
    "cityName": "Seoul",
    "country": "South Korea",
    "region": "Seoul",
    "latitude": 37.5665,
    "longitude": 126.9780,
    "imageUrl": "https://...",
    "hotels": 1200
  }
]
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `destId` | String | 목적지 ID — 이후 검색에 필수 |
| `destType` | String | 목적지 타입 (`CITY`, `LANDMARK`, `DISTRICT` 등) |
| `name` | String | 목적지 이름 |
| `label` | String | UI 표시용 전체 레이블 |
| `cityName` | String | 도시 이름 |
| `country` | String | 국가 |
| `region` | String | 지역/도 |
| `latitude` | Double | 위도 |
| `longitude` | Double | 경도 |
| `imageUrl` | String | 대표 이미지 URL |
| `hotels` | Integer | 해당 목적지 내 호텔 수 |

### 프론트 구현 팁

```typescript
// 디바운스 300ms 권장
const results = await fetch(`/supply/hotels/destinations?query=${encodeURIComponent(query)}`);
// 사용자가 항목 선택 시 destId, destType(=searchType) 저장
const selected = { destId: item.destId, searchType: item.destType };
```

---

## 2. 호텔 검색

> 검색하기 버튼 클릭 시 호출. 호텔 목록 및 가격 정보를 반환.

```
GET /supply/hotels/offers
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 | 예시 |
|---|---|---|---|---|---|
| `destId` | String | ✅ | — | destinations에서 획득한 목적지 ID | `"-716357"` |
| `searchType` | String | ✅ | — | destinations에서 획득한 목적지 타입 | `"CITY"` |
| `arrivalDate` | LocalDate | ✅ | — | 체크인 날짜 (YYYY-MM-DD) | `"2026-06-01"` |
| `departureDate` | LocalDate | ✅ | — | 체크아웃 날짜 (YYYY-MM-DD) | `"2026-06-03"` |
| `adults` | Integer | ❌ | `1` | 성인 수 | `2` |
| `roomQty` | Integer | ❌ | `1` | 객실 수 | `1` |
| `childrenAge` | String | ❌ | `"0"` | 어린이 나이 (쉼표 구분) | `"5,8"` |
| `pageNumber` | Integer | ❌ | `1` | 페이지 번호 | `2` |
| `sortBy` | String | ❌ | `"price"` | 정렬 기준 (sort-options의 `id` 값 사용) | `"review_score"` |
| `priceMin` | Float | ❌ | `0` | 최소 가격 필터 | `50000` |
| `priceMax` | Float | ❌ | `0` | 최대 가격 필터 (0 = 제한 없음) | `300000` |
| `categoriesFilter` | String | ❌ | `"popular"` | 카테고리 필터 (filter-options의 값 사용) | `"free_cancellation"` |
| `currencyCode` | String | ❌ | `"KRW"` | 통화 코드 | `"KRW"` |
| `languageCode` | String | ❌ | `"ko"` | 언어 코드 | `"ko"` |
| `units` | String | ❌ | `"METRIC"` | 거리 단위 (`METRIC` \| `IMPERIAL`) | `"METRIC"` |
| `temperatureUnit` | String | ❌ | `"CELSIUS"` | 온도 단위 (`CELSIUS` \| `FAHRENHEIT`) | `"CELSIUS"` |
| `supplier` | String | ❌ | `"booking"` | 공급자 키 | `"booking"` |

### Response

Booking.com RapidAPI 원본 JSON을 그대로 반환합니다. 주요 필드:

```json
{
  "data": {
    "hotels": [
      {
        "hotel_id": 123456,
        "name": "호텔 이름",
        "review_score": 8.5,
        "review_score_word": "Very Good",
        "review_nr": 2341,
        "class": 4,
        "main_photo_url": "https://...",
        "url": "https://www.booking.com/hotel/...",
        "min_total_price": 150000,
        "composite_price_breakdown": {
          "gross_amount": { "value": 150000, "currency": "KRW" }
        },
        "latitude": 37.5665,
        "longitude": 126.9780,
        "distance_to_cc": "1.2",
        "checkin": { "from": "15:00" },
        "checkout": { "until": "11:00" },
        "is_free_cancellable": true,
        "badges": [{ "id": "free_cancellation", "text": "무료 취소" }]
      }
    ],
    "count": 150,
    "primary_count": 50
  }
}
```

### URL 탭 연동 (`/api/v1/hotels/offers` → URL 직접 입력)

UI의 `URL` 탭은 호텔 직접 URL 입력 방식으로, 별도 API 없이 프론트에서 처리합니다.

---

## 3. 정렬 옵션 조회

> 검색 결과 화면 진입 시 1회 호출. 정렬 드롭다운 목록을 구성하는 데 사용.

```
GET /supply/hotels/sort-options
```

### Query Parameters

`offers`와 동일한 검색 조건을 전달합니다.

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `destId` | String | ✅ | 목적지 ID |
| `searchType` | String | ✅ | 목적지 타입 |
| `arrivalDate` | LocalDate | ✅ | 체크인 날짜 |
| `departureDate` | LocalDate | ✅ | 체크아웃 날짜 |
| `adults` | Integer | ❌ | 성인 수 (기본값 `1`) |
| `roomQty` | Integer | ❌ | 객실 수 (기본값 `1`) |
| `childrenAge` | String | ❌ | 어린이 나이 |

### Response

```json
[
  { "id": "price", "title": "가격 낮은 순" },
  { "id": "review_score", "title": "평점 높은 순" },
  { "id": "class", "title": "성급 높은 순" },
  { "id": "distance", "title": "중심지와 가까운 순" }
]
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | String | `offers`의 `sortBy` 파라미터에 사용하는 값 |
| `title` | String | UI에 표시할 정렬 옵션 이름 |

---

## 4. 필터 옵션 조회

> 검색 결과 화면 진입 시 1회 호출. 필터 패널(무료 취소, 성급, 가격대 등)을 구성.

```
GET /supply/hotels/filter-options
```

### Query Parameters

`sort-options`와 동일 + `categoriesFilter` 추가:

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `destId` | String | ✅ | 목적지 ID |
| `searchType` | String | ✅ | 목적지 타입 |
| `arrivalDate` | LocalDate | ✅ | 체크인 날짜 |
| `departureDate` | LocalDate | ✅ | 체크아웃 날짜 |
| `adults` | Integer | ❌ | 성인 수 |
| `roomQty` | Integer | ❌ | 객실 수 |
| `childrenAge` | String | ❌ | 어린이 나이 |
| `categoriesFilter` | String | ❌ | 초기 조회 시 빈값, 필터 선택 후 재조회 시 선택값 전달 |

### Response

Booking.com RapidAPI 원본 JSON 반환. 주요 필터 구조:

```json
[
  {
    "id": "class",
    "title": "성급",
    "filters": [
      { "id": "class::1", "title": "1성급", "count": 12 },
      { "id": "class::4", "title": "4성급", "count": 85 }
    ]
  },
  {
    "id": "free_cancellation",
    "title": "무료 취소",
    "filters": [
      { "id": "free_cancellation::1", "title": "무료 취소 가능", "count": 320 }
    ]
  },
  {
    "id": "price",
    "title": "1박 요금",
    "filters": [
      { "id": "price::0-50000", "title": "~50,000원", "count": 45 }
    ]
  }
]
```

각 필터 항목의 `id` 값을 `offers`의 `categoriesFilter` 파라미터로 전달합니다.

---

## 전체 검색 플로우 예시

```typescript
// 1. 목적지 자동완성
const destinations = await fetch(`/supply/hotels/destinations?query=tokyo`);
const selected = destinations[0]; // 사용자가 선택한 항목
// → { destId: "-246227", destType: "CITY", name: "Tokyo", ... }

// 검색 공통 파라미터
const baseParams = new URLSearchParams({
  destId: selected.destId,        // "-246227"
  searchType: selected.destType,  // "CITY"
  arrivalDate: "2026-06-01",
  departureDate: "2026-06-03",
  adults: "2",
  roomQty: "1",
});

// 2. 검색 버튼 클릭 시 3개 API 병렬 호출
const [offers, sortOptions, filterOptions] = await Promise.all([
  fetch(`/supply/hotels/offers?${baseParams}&sortBy=price`),
  fetch(`/supply/hotels/sort-options?${baseParams}`),
  fetch(`/supply/hotels/filter-options?${baseParams}`),
]);

// 3. 필터 변경 시 offers 재호출
const filtered = await fetch(
  `/supply/hotels/offers?${baseParams}&sortBy=review_score&categoriesFilter=free_cancellation::1`
);

// 4. 페이지네이션
const page2 = await fetch(`/supply/hotels/offers?${baseParams}&pageNumber=2`);
```

---

## UI 탭별 파라미터 매핑

| UI 요소 | API 파라미터 | 비고 |
|---|---|---|
| 목적지 입력창 | `query` → `destinations` → `destId` + `searchType` | 자동완성 후 ID로 변환 |
| 체크인 날짜 | `arrivalDate` | `YYYY-MM-DD` 형식 |
| 체크아웃 날짜 | `departureDate` | `YYYY-MM-DD` 형식 |
| 성인 N명, N개 | `adults`, `roomQty` | |
| 무료 취소 토글 | `categoriesFilter=free_cancellation::1` | filter-options의 id 값 |
| 4성급+ 토글 | `categoriesFilter=class::4` | filter-options의 id 값 |
| 정렬 드롭다운 | `sortBy` | sort-options의 id 값 |

---

## 에러 코드

| HTTP | 상황 |
|---|---|
| 400 | 필수 파라미터 누락 (`destId`, `arrivalDate` 등) |
| 500 | Booking.com RapidAPI 호출 실패 (외부 API 한도 초과 등) |

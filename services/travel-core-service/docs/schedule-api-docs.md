# Schedule API 문서

> **Base URL** `https://api.sofly.co.kr`
> **인증 방식** JWT Bearer Token (`Authorization: Bearer {token}`)
> **Content-Type** `application/json`

---

## 공통 사항

- 모든 요청에 `Authorization: Bearer {token}` 헤더 필요
- `category` 값: `ACCOMMODATION` | `RESTAURANT` | `CAFE` | `ATTRACTION` | `TRANSPORT`
- `visitTime` 형식: `"HH:mm"` (예: `"09:30"`)
- 날짜/시간 응답: ISO 8601 (`"2026-05-12T10:00:00"`)

---

## 일정 (Schedule)

### 1. 일정 목록 조회

워크스페이스에 속한 모든 일정 버전 목록을 조회합니다 (아이템 미포함).

```
GET /api/v1/schedules?workspaceId={workspaceId}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `workspaceId` | Long | O | 워크스페이스 ID |

**응답 예시** `200 OK`

```json
[
  {
    "id": 1,
    "title": "1일차 일정",
    "version": 1,
    "itemCount": 5,
    "createdAt": "2026-05-12T10:00:00"
  },
  {
    "id": 2,
    "title": "수정본",
    "version": 2,
    "itemCount": 7,
    "createdAt": "2026-05-12T12:00:00"
  }
]
```

---

### 2. 일정 상세 조회

일정 ID로 아이템을 포함한 상세 정보를 조회합니다. `itemsByDay`는 일차(day)를 키로 그룹핑됩니다.

```
GET /api/v1/schedules/{scheduleId}
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `scheduleId` | Long | 일정 ID |

**응답 예시** `200 OK`

```json
{
  "id": 1,
  "workspaceId": 10,
  "title": "제주도 여행",
  "version": 1,
  "itemsByDay": {
    "1": [
      {
        "id": 101,
        "day": 1,
        "orderIndex": 0,
        "visitTime": "09:30",
        "category": "ATTRACTION",
        "name": "경복궁",
        "address": "서울특별시 종로구 사직로 161",
        "latitude": 37.5796,
        "longitude": 126.977,
        "placeId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
        "photoReference": "AXCi2Q...",
        "memo": "오전에 방문 추천",
        "deepLinkUrl": null,
        "estimatedCost": 3000.0,
        "deepLinkClickCount": 0,
        "createdAt": "2026-05-12T10:00:00",
        "updatedAt": "2026-05-12T10:00:00"
      }
    ],
    "2": [
      {
        "id": 102,
        "day": 2,
        "orderIndex": 0,
        "visitTime": "12:00",
        "category": "RESTAURANT",
        "name": "맛집",
        "address": "서울특별시 마포구",
        "latitude": 37.55,
        "longitude": 126.92,
        "placeId": null,
        "photoReference": null,
        "memo": null,
        "deepLinkUrl": null,
        "estimatedCost": 15000.0,
        "deepLinkClickCount": 0,
        "createdAt": "2026-05-12T10:00:00",
        "updatedAt": "2026-05-12T10:00:00"
      }
    ]
  },
  "createdAt": "2026-05-12T10:00:00",
  "updatedAt": "2026-05-12T10:00:00"
}
```

---

### 3. 최신 일정 조회

워크스페이스의 가장 최근 버전 일정을 조회합니다. 진입 시 기본으로 불러올 일정에 사용합니다.

```
GET /api/v1/schedules/latest?workspaceId={workspaceId}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `workspaceId` | Long | O | 워크스페이스 ID |

**응답** → [일정 상세 조회](#2-일정-상세-조회)와 동일

---

### 4. 일정 생성

새 일정을 생성합니다. AI 채팅에서 일정이 확정(저장)될 때 호출됩니다.

```
POST /api/v1/schedules
```

**Request Body**

```json
{
  "workspaceId": 10,
  "title": "제주도 3박 4일",
  "items": [
    {
      "day": 1,
      "orderIndex": 0,
      "visitTime": "09:30",
      "category": "ATTRACTION",
      "name": "경복궁",
      "address": "서울특별시 종로구 사직로 161",
      "latitude": 37.5796,
      "longitude": 126.977,
      "placeId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
      "photoReference": "AXCi2Q...",
      "memo": "오전에 방문 추천",
      "deepLinkUrl": null,
      "estimatedCost": 3000
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `workspaceId` | Long | O | 워크스페이스 ID |
| `title` | String | X | 일정 제목 (미입력 시 자동 생성) |
| `items` | Array | O | 아이템 목록 (최소 1개) |
| `items[].day` | Integer | O | 일차 (1 이상) |
| `items[].orderIndex` | Integer | O | 해당 일차 내 순서 (0 이상) |
| `items[].visitTime` | String | X | 방문 시각 `"HH:mm"` |
| `items[].category` | String | O | `ACCOMMODATION` `RESTAURANT` `CAFE` `ATTRACTION` `TRANSPORT` |
| `items[].name` | String | O | 장소명 |
| `items[].address` | String | X | 주소 |
| `items[].latitude` | Double | X | 위도 |
| `items[].longitude` | Double | X | 경도 |
| `items[].placeId` | String | X | Google Places ID |
| `items[].photoReference` | String | X | Google Places 사진 레퍼런스 |
| `items[].memo` | String | X | 메모 |
| `items[].deepLinkUrl` | String | X | 딥링크 URL |
| `items[].estimatedCost` | Double | X | 예상 비용 |

**응답** → [일정 상세 조회](#2-일정-상세-조회)와 동일

---

### 5. 일정 포크 (복사)

기존 일정을 복사해 새 버전을 생성합니다. 일정을 수정하기 전 복사본을 만들 때 사용합니다.

```
POST /api/v1/schedules/{scheduleId}/fork?title={title}
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `scheduleId` | Long | 복사할 원본 일정 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `title` | String | X | 새 일정 제목 (미입력 시 원본 제목 사용) |

**응답** → [일정 상세 조회](#2-일정-상세-조회)와 동일

---

### 6. 일정 제목 수정

```
PATCH /api/v1/schedules/{scheduleId}/title?title={title}
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `scheduleId` | Long | 일정 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `title` | String | O | 변경할 제목 |

**응답** → [일정 상세 조회](#2-일정-상세-조회)와 동일

---

### 7. 일정 삭제

```
DELETE /api/v1/schedules/{scheduleId}
```

**응답** `204 No Content`

---

### 8. 지도 핀 조회

좌표(위경도)가 등록된 아이템을 일차별로 그룹핑해 반환합니다. 지도 탭에서 핀을 표시할 때 사용합니다.

```
GET /api/v1/schedules/{scheduleId}/map
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `scheduleId` | Long | 일정 ID |

**응답 예시** `200 OK`

```json
{
  "days": [
    {
      "day": 1,
      "pins": [
        {
          "scheduleItemId": 101,
          "name": "경복궁",
          "category": "ATTRACTION",
          "latitude": 37.5796,
          "longitude": 126.977,
          "placeId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
          "photoReference": "AXCi2Q...",
          "visitTime": "09:30:00"
        }
      ]
    },
    {
      "day": 2,
      "pins": [
        {
          "scheduleItemId": 105,
          "name": "남산타워",
          "category": "ATTRACTION",
          "latitude": 37.5512,
          "longitude": 126.9882,
          "placeId": null,
          "photoReference": null,
          "visitTime": "14:00:00"
        }
      ]
    }
  ]
}
```

> 좌표(latitude, longitude)가 null인 아이템은 응답에 포함되지 않습니다.

---

## 일정 아이템 (Schedule Item)

### 9. 아이템 추가

기존 일정에 아이템 1개를 추가합니다.

```
POST /api/v1/schedules/{scheduleId}/items
```

**Request Body**

```json
{
  "day": 1,
  "orderIndex": 2,
  "visitTime": "14:00",
  "category": "RESTAURANT",
  "name": "맛집",
  "address": "서울특별시 마포구",
  "latitude": 37.55,
  "longitude": 126.92,
  "placeId": null,
  "photoReference": null,
  "memo": "점심 추천",
  "deepLinkUrl": null,
  "estimatedCost": 15000
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `day` | Integer | O | 일차 (1 이상) |
| `orderIndex` | Integer | O | 순서 (0 이상) |
| `visitTime` | String | X | `"HH:mm"` |
| `category` | String | O | 카테고리 |
| `name` | String | O | 장소명 |
| `address` | String | X | 주소 |
| `latitude` | Double | X | 위도 |
| `longitude` | Double | X | 경도 |
| `placeId` | String | X | Google Places ID |
| `photoReference` | String | X | Google Places 사진 레퍼런스 |
| `memo` | String | X | 메모 |
| `deepLinkUrl` | String | X | 딥링크 URL |
| `estimatedCost` | Double | X | 예상 비용 |

**응답 예시** `200 OK`

```json
{
  "id": 103,
  "day": 1,
  "orderIndex": 2,
  "visitTime": "14:00",
  "category": "RESTAURANT",
  "name": "맛집",
  "address": "서울특별시 마포구",
  "latitude": 37.55,
  "longitude": 126.92,
  "placeId": null,
  "photoReference": null,
  "memo": "점심 추천",
  "deepLinkUrl": null,
  "estimatedCost": 15000.0,
  "deepLinkClickCount": 0,
  "createdAt": "2026-05-12T14:00:00",
  "updatedAt": "2026-05-12T14:00:00"
}
```

---

### 10. 아이템 수정

아이템의 내용을 수정합니다. 모든 필드를 전송해야 합니다 (`category`만 필수).

```
PATCH /api/v1/schedules/{scheduleId}/items/{itemId}
```

**Request Body**

```json
{
  "visitTime": "15:00",
  "memo": "수정된 메모",
  "category": "RESTAURANT",
  "address": "서울특별시 강남구",
  "estimatedCost": 20000,
  "name": "수정된 장소명",
  "placeId": null,
  "photoReference": null,
  "latitude": 37.5,
  "longitude": 127.0
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `category` | String | O | 카테고리 |
| `name` | String | X | 장소명 |
| `visitTime` | String | X | `"HH:mm"` |
| `memo` | String | X | 메모 |
| `address` | String | X | 주소 |
| `estimatedCost` | Double | X | 예상 비용 |
| `placeId` | String | X | Google Places ID |
| `photoReference` | String | X | Google Places 사진 레퍼런스 |
| `latitude` | Double | X | 위도 |
| `longitude` | Double | X | 경도 |

**응답** → [아이템 추가](#9-아이템-추가) 응답과 동일

---

### 11. 아이템 위치 이동 (드래그 앤 드롭)

아이템을 다른 일차 또는 다른 순서로 이동합니다. 서버가 나머지 아이템 순서를 자동 조정합니다.

```
PATCH /api/v1/schedules/{scheduleId}/items/{itemId}/position
```

**Request Body**

```json
{
  "targetDay": 2,
  "targetOrderIndex": 0
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `targetDay` | Integer | O | 이동할 일차 (1 이상) |
| `targetOrderIndex` | Integer | O | 이동할 순서 (0 이상) |

**응답** `204 No Content`

> 이동 후 변경된 일정 전체를 갱신하려면 [일정 상세 조회](#2-일정-상세-조회)를 다시 호출합니다.

---

### 12. 아이템 삭제

```
DELETE /api/v1/schedules/{scheduleId}/items/{itemId}
```

**응답** `204 No Content`

---

### 13. 딥링크 클릭 추적

아이템의 딥링크 버튼 클릭 이벤트를 서버에 기록합니다.

```
POST /api/v1/schedules/{scheduleId}/items/{itemId}/deeplink-click
```

**응답** `204 No Content`

---

## 에러 응답

```json
{
  "success": false,
  "message": "일정을 찾을 수 없습니다.",
  "data": null
}
```

| HTTP | 설명 |
|------|------|
| `401` | 인증 토큰 없음 또는 만료 |
| `403` | 다른 일정의 아이템에 접근 시도 |
| `404` | 일정 또는 아이템을 찾을 수 없음 |
| `400` | 요청 값 유효성 오류 (필수 필드 누락 등) |

---

## 프론트엔드 연동 가이드

### 일정 페이지 진입 플로우

```
워크스페이스 진입
  → GET /api/v1/schedules/latest?workspaceId={id}  // 최신 일정 자동 로드
  → 없으면 빈 상태 표시 (AI 채팅으로 생성 유도)
```

### 일정 버전 탭 전환

```
GET /api/v1/schedules?workspaceId={id}  // 버전 목록 조회
  → 버전 탭 선택 시
GET /api/v1/schedules/{scheduleId}       // 해당 버전 상세 조회
```

### 아이템 드래그 앤 드롭

```
드롭 이벤트 발생
  → PATCH /api/v1/schedules/{scheduleId}/items/{itemId}/position
  → 완료 후 GET /api/v1/schedules/{scheduleId} 로 전체 재조회
```

### AI 채팅 → 일정 저장

```
사용자가 "저장해줘" / "확정해줘" 입력
  → AI가 JSON 형태로 일정 반환
  → POST /api/v1/schedules  // 일정 생성
  → 생성된 일정으로 화면 전환
```

### category 표시 매핑

| 값 | 한국어 | 아이콘 제안 |
|----|--------|------------|
| `ACCOMMODATION` | 숙소 | 🏨 |
| `RESTAURANT` | 식당 | 🍽️ |
| `CAFE` | 카페 | ☕ |
| `ATTRACTION` | 관광지 | 🎯 |
| `TRANSPORT` | 교통 | 🚌 |

# 여행기(TravelLog) API — 프론트엔드 연동 가이드

## 개요

여행기는 워크스페이스 단위로 관리되는 마크다운 기반 여행 일지다.  
사진은 두 가지 방식으로 첨부할 수 있다.

- **직접 업로드**: 새 파일을 S3에 올리고 즉시 여행기에 첨부
- **앨범 연결**: 워크스페이스 앨범에 이미 올라간 사진을 `photoId`로 참조

---

## 공통 사항

| 항목 | 값 |
|------|-----|
| Base URL | `http://localhost:8080` |
| 인증 | `Authorization: Bearer <JWT>` 헤더 필수 |
| Content-Type | `application/json` (파일 업로드 제외) |

### 공통 응답 래퍼

```json
{
  "success": true,
  "data": { ... }
}
```

실패 시:

```json
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "오류 메시지"
}
```

---

## API 목록

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/api/workspaces/{workspaceId}/travellogs` | 여행기 목록 조회 (content 제외) |
| `GET` | `/api/workspaces/{workspaceId}/travellogs/{logId}` | 여행기 단건 조회 (사진 포함) |
| `POST` | `/api/workspaces/{workspaceId}/travellogs` | 여행기 생성 |
| `PATCH` | `/api/workspaces/{workspaceId}/travellogs/{logId}` | 여행기 수정 (부분 수정) |
| `DELETE` | `/api/workspaces/{workspaceId}/travellogs/{logId}` | 여행기 삭제 |
| `POST` | `/api/workspaces/{workspaceId}/travellogs/{logId}/photos/upload` | 새 사진 업로드 후 첨부 |
| `POST` | `/api/workspaces/{workspaceId}/travellogs/{logId}/photos` | 앨범 사진 첨부 |
| `DELETE` | `/api/workspaces/{workspaceId}/travellogs/{logId}/photos` | 사진 첨부 해제 |

---

## 데이터 타입 참고

### Weather (날씨)

```
SUNNY | CLOUDY | RAINY | SNOWY
```

### TravellogSummaryResponse (목록 조회 시 반환)

```typescript
interface TravellogSummaryResponse {
  id: number;
  day: number | null;        // 여행 N일차 (선택)
  travelDate: string | null; // "YYYY-MM-DD" (선택)
  title: string;
  weather: "SUNNY" | "CLOUDY" | "RAINY" | "SNOWY" | null;
  photoCount: number;
  createdAt: string;         // ISO 8601
}
```

### TravellogResponse (단건 조회·생성·수정 시 반환)

```typescript
interface TravellogResponse {
  id: number;
  day: number | null;
  travelDate: string | null;  // "YYYY-MM-DD"
  title: string;
  content: string;            // 마크다운 원문
  weather: "SUNNY" | "CLOUDY" | "RAINY" | "SNOWY" | null;
  workspaceId: number;
  authorId: number;
  authorNickname: string;
  photos: PhotoResponse[];
  createdAt: string;
  updatedAt: string;
}

interface PhotoResponse {
  id: number;
  url: string;               // S3 퍼블릭 URL
  uploadedById: number;
  uploadedByNickname: string;
  takenAt: string | null;    // "YYYY-MM-DD", EXIF 촬영일
  latitude: number | null;
  longitude: number | null;
  matchedDay: number | null; // 일정 N일차 자동 매칭 결과
  createdAt: string;
}
```

---

## 엔드포인트 상세

### 1. 여행기 목록 조회

```
GET /api/workspaces/{workspaceId}/travellogs
```

`content`(본문)는 포함되지 않는다. 목록 렌더링용 요약 정보만 반환한다.

**응답 예시**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "day": 1,
      "travelDate": "2025-07-01",
      "title": "도쿄 첫째 날",
      "weather": "SUNNY",
      "photoCount": 3,
      "createdAt": "2025-07-01T10:00:00"
    },
    {
      "id": 2,
      "day": 2,
      "travelDate": "2025-07-02",
      "title": "아사쿠사 탐방",
      "weather": "CLOUDY",
      "photoCount": 0,
      "createdAt": "2025-07-02T09:30:00"
    }
  ]
}
```

---

### 2. 여행기 단건 조회

```
GET /api/workspaces/{workspaceId}/travellogs/{logId}
```

`content`와 첨부 사진 전체를 반환한다.

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "day": 1,
    "travelDate": "2025-07-01",
    "title": "도쿄 첫째 날",
    "content": "## 시작\n오늘은 나리타에 도착했다...",
    "weather": "SUNNY",
    "workspaceId": 10,
    "authorId": 42,
    "authorNickname": "상민",
    "photos": [
      {
        "id": 101,
        "url": "https://sofly-bucket.s3.ap-northeast-2.amazonaws.com/workspaces/10/photos/uuid.jpg",
        "uploadedById": 42,
        "uploadedByNickname": "상민",
        "takenAt": "2025-07-01",
        "latitude": 35.6895,
        "longitude": 139.6917,
        "matchedDay": 1,
        "createdAt": "2025-07-01T11:00:00"
      }
    ],
    "createdAt": "2025-07-01T10:00:00",
    "updatedAt": "2025-07-01T12:00:00"
  }
}
```

---

### 3. 여행기 생성

```
POST /api/workspaces/{workspaceId}/travellogs
Content-Type: application/json
```

**요청 바디**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | string | ✅ | 제목 (빈 값 불가) |
| `content` | string | ✅ | 본문 마크다운 (빈 값 불가) |
| `day` | number | - | 여행 N일차 |
| `travelDate` | string | - | `"YYYY-MM-DD"` |
| `weather` | string | - | `SUNNY \| CLOUDY \| RAINY \| SNOWY` |

```json
{
  "day": 1,
  "travelDate": "2025-07-01",
  "title": "도쿄 첫째 날",
  "content": "## 시작\n오늘은 나리타에 도착했다...",
  "weather": "SUNNY"
}
```

**응답**: `201 Created` + `TravellogResponse`

---

### 4. 여행기 수정

```
PATCH /api/workspaces/{workspaceId}/travellogs/{logId}
Content-Type: application/json
```

**null로 보낸 필드는 변경되지 않는다.** 바꾸고 싶은 필드만 포함하면 된다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `title` | string \| null | 빈 문자열 불가 (최소 1자) |
| `content` | string \| null | 빈 문자열 불가 (최소 1자) |
| `day` | number \| null | - |
| `travelDate` | string \| null | `"YYYY-MM-DD"` |
| `weather` | string \| null | - |

```json
{
  "title": "도쿄 첫날 — 수정본",
  "weather": "CLOUDY"
}
```

**응답**: `200 OK` + `TravellogResponse`

---

### 5. 여행기 삭제

```
DELETE /api/workspaces/{workspaceId}/travellogs/{logId}
```

**응답**: `204 No Content`

---

### 6. 새 사진 업로드 후 첨부

```
POST /api/workspaces/{workspaceId}/travellogs/{logId}/photos/upload
Content-Type: multipart/form-data
```

파일을 S3에 업로드하고 즉시 여행기에 첨부한다.  
앨범에도 동시 등록된다.

| 제약 | 값 |
|------|----|
| 폼 필드명 | `files` |
| 허용 형식 | jpeg / png / webp / heic |
| 파일당 최대 크기 | 10MB |
| 요청당 최대 파일 수 | 20개 |

**요청 예시 (fetch)**

```javascript
const formData = new FormData();
files.forEach(file => formData.append('files', file));

const response = await fetch(
  `/api/workspaces/${workspaceId}/travellogs/${logId}/photos/upload`,
  {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
    // Content-Type은 브라우저가 boundary 포함해서 자동 설정
  }
);
```

**응답**: `200 OK` + `TravellogResponse` (업로드된 사진이 `photos` 배열에 포함됨)

---

### 7. 앨범 사진 첨부

```
POST /api/workspaces/{workspaceId}/travellogs/{logId}/photos
Content-Type: application/json
```

워크스페이스 앨범에 이미 존재하는 사진을 `photoId`로 참조해 여행기에 첨부한다.  
사진 파일을 다시 업로드하지 않는다.

**요청 바디**

```json
{
  "photoIds": [101, 102, 103]
}
```

**응답**: `200 OK` + `TravellogResponse`

---

### 8. 사진 첨부 해제

```
DELETE /api/workspaces/{workspaceId}/travellogs/{logId}/photos
Content-Type: application/json
```

여행기에서 사진을 떼어낸다. **앨범에서는 삭제되지 않는다.**

**요청 바디**

```json
{
  "photoIds": [101, 102]
}
```

**응답**: `200 OK` + `TravellogResponse` (해제된 사진이 `photos` 배열에서 빠진 상태)

---

## 프론트 연동 시나리오

### 시나리오 A. 여행기 목록 → 상세 진입

```
1. GET /api/workspaces/{workspaceId}/travellogs
   → TravellogSummaryResponse[] 렌더링 (제목, 날짜, 날씨 아이콘, 사진 수 뱃지)

2. 카드 클릭
   → GET /api/workspaces/{workspaceId}/travellogs/{logId}
   → content(마크다운)를 렌더링 라이브러리로 파싱하여 표시
   → photos[] 배열로 사진 갤러리 표시
```

---

### 시나리오 B. 여행기 작성

```
1. 사용자가 제목·본문·날짜·날씨 입력

2. POST /api/workspaces/{workspaceId}/travellogs
   → 응답 data.id 를 저장 (이후 사진 첨부에 사용)

3. (선택) 사진 첨부
   → 새 파일이 있으면 시나리오 D 실행
   → 앨범 사진을 연결하려면 시나리오 E 실행
```

---

### 시나리오 C. 여행기 수정

```
1. GET /api/workspaces/{workspaceId}/travellogs/{logId}
   → 기존 값으로 폼 초기화

2. 사용자가 수정할 필드만 변경

3. PATCH /api/workspaces/{workspaceId}/travellogs/{logId}
   바디: 변경된 필드만 포함 (나머지는 omit 또는 null)
   → 응답으로 최신 TravellogResponse 수신, UI 갱신
```

**주의**: `null`을 보내면 변경되지 않는다. 필드를 지우고 싶어도 현재 API 설계상 빈 값으로 덮어쓸 수 없다. (추후 서버 정책 확인 필요)

---

### 시나리오 D. 새 사진 업로드 후 여행기에 첨부

```
1. 파일 선택 (jpeg/png/webp/heic, 10MB 이하, 최대 20개)

2. POST /api/workspaces/{workspaceId}/travellogs/{logId}/photos/upload
   multipart: files = [File, File, ...]

3. 응답 data.photos[] 로 갤러리 갱신

※ 업로드된 사진은 워크스페이스 앨범에도 자동 등록됨
```

---

### 시나리오 E. 앨범에서 사진 골라 여행기에 첨부

```
1. GET /api/workspaces/{workspaceId}/albums/{albumId}/photos
   (앨범 API — 별도 문서 참고)
   → 앨범 사진 그리드 표시

2. 사용자가 사진 선택 (photoId[] 수집)

3. POST /api/workspaces/{workspaceId}/travellogs/{logId}/photos
   Body: { "photoIds": [101, 102] }

4. 응답 data.photos[] 로 갤러리 갱신
```

---

### 시나리오 F. 사진 첨부 해제

```
1. 여행기 상세 화면에서 사진 선택 → "해제" 버튼 클릭

2. DELETE /api/workspaces/{workspaceId}/travellogs/{logId}/photos
   Body: { "photoIds": [101] }

3. 응답 data.photos[] 기준으로 갤러리 갱신

※ 앨범에서는 삭제되지 않으므로 앨범 뷰는 별도로 갱신 불필요
```

---

### 시나리오 G. 여행기 삭제

```
1. DELETE /api/workspaces/{workspaceId}/travellogs/{logId}
   → 204 No Content

2. 목록 페이지로 리디렉트 또는 목록 재조회
```

---

## 에러 케이스 정리

| 상황 | HTTP 상태 | 대응 |
|------|-----------|------|
| JWT 없음 / 만료 | `401` | 로그인 페이지 이동 |
| 워크스페이스 접근 권한 없음 | `403` | 권한 오류 토스트 |
| 존재하지 않는 logId | `404` | "삭제된 여행기" 안내 |
| title/content 빈 값 | `400` | 폼 validation 메시지 표시 |
| 파일 형식·크기 위반 | `400` | "지원되지 않는 형식 또는 10MB 초과" 안내 |
| photoIds 비어있음 | `400` | 사진을 1개 이상 선택하도록 안내 |

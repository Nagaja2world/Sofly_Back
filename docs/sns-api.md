# SNS API 연동 가이드

> 대상: 프론트엔드 개발자  
> Base URL: `https://api.sofly.co.kr`  
> 인증이 필요한 API는 `Authorization: Bearer <JWT>` 헤더를 포함하세요.  
> 인증 선택 API는 비로그인 상태에서도 호출 가능하지만, 로그인 상태에서는 `isLiked` / `isFollowing` 값이 채워집니다.

---

## 목차

1. [공개 범위(Visibility) 개념](#1-공개-범위visibility-개념)
2. [피드](#2-피드)
3. [팔로우](#3-팔로우)
4. [공개 프로필](#4-공개-프로필)
5. [워크스페이스 검색](#5-워크스페이스-검색)
6. [좋아요](#6-좋아요)
7. [댓글](#7-댓글)
8. [타입 정의](#8-타입-정의)
9. [에러 코드](#9-에러-코드)

---

## 1. 공개 범위(Visibility) 개념

SNS 기능 전반에 걸쳐 워크스페이스의 `visibility` 값이 접근 권한을 결정합니다.

| 값 | 접근 가능 대상 | SNS 피드/검색 노출 |
|---|---|---|
| `PUBLIC` | 모든 사용자 (비로그인 포함) | O |
| `FOLLOWERS_ONLY` | 소유자 + 팔로워 | X (검색에서 제외) |
| `PRIVATE` | 워크스페이스 멤버만 | X |

> 워크스페이스 공개 범위는 워크스페이스 수정 API(`PUT /api/v1/workspaces/{id}`)의 `visibility` 필드로 설정합니다.

---

## 2. 피드

### 알고리즘 피드 조회

내가 팔로우하는 유저의 공개 워크스페이스를 우선으로, 전체 공개 워크스페이스를 점수 기반으로 정렬하여 반환합니다.

> **점수 계산:** `좋아요 수 × 3 + 댓글 수 × 2 + 최근 7일 이내 생성 보너스(+10)`

```
GET /api/sns/feed?page=0&size=20
Authorization: Bearer <JWT>  (필수)
```

**Query Parameters**

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `page` | `0` | 페이지 번호 |
| `size` | `20` | 페이지 크기 |

**Response `200 OK`**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 6,
        "title": "도쿄가실 분~!!",
        "destination": "도쿄",
        "countryCode": "JP",
        "startDate": "2026-05-26",
        "endDate": "2026-05-28",
        "headcount": 4,
        "coverImageUrl": "https://...",
        "visibility": "PUBLIC",
        "author": {
          "userId": 4,
          "nickname": "여행왕",
          "profileImageUrl": "https://..."
        },
        "likeCount": 12,
        "commentCount": 3,
        "isLiked": false,
        "createdAt": "2026-05-11T18:35:59"
      }
    ],
    "totalElements": 45,
    "totalPages": 3,
    "number": 0,
    "size": 20,
    "first": true,
    "last": false
  }
}
```

---

## 3. 팔로우

### 팔로우

```
POST /api/sns/users/{targetUserId}/follow
Authorization: Bearer <JWT>  (필수)
```

**Response `200 OK`**
```json
{ "success": true, "data": null }
```

---

### 언팔로우

```
DELETE /api/sns/users/{targetUserId}/follow
Authorization: Bearer <JWT>  (필수)
```

**Response `204 No Content`**

---

### 팔로우 통계 조회

```
GET /api/sns/users/{targetUserId}/follow-stats
Authorization: Bearer <JWT>  (선택)
```

**Response `200 OK`**

```json
{
  "success": true,
  "data": {
    "followerCount": 42,
    "followingCount": 18,
    "isFollowing": true
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `followerCount` | `number` | 나를 팔로우하는 수 |
| `followingCount` | `number` | 내가 팔로우하는 수 |
| `isFollowing` | `boolean` | 내가 이 유저를 팔로우 중인지 (비로그인 시 `false`) |

---

## 4. 공개 프로필

특정 유저의 프로필과 그 유저의 공개 워크스페이스 목록을 함께 조회합니다.

```
GET /api/sns/users/{targetUserId}/profile?page=0&size=12
Authorization: Bearer <JWT>  (선택)
```

**Query Parameters**

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `page` | `0` | 워크스페이스 페이지 번호 |
| `size` | `12` | 페이지 크기 |
| `sort` | `createdAt,DESC` | 정렬 기준 |

**Response `200 OK`**

```json
{
  "success": true,
  "data": {
    "userId": 4,
    "nickname": "여행왕",
    "profileImageUrl": "https://...",
    "followerCount": 42,
    "followingCount": 18,
    "isFollowing": true,
    "publicWorkspaces": {
      "content": [
        {
          "id": 6,
          "title": "도쿄가실 분~!!",
          "destination": "도쿄",
          "countryCode": "JP",
          "startDate": "2026-05-26",
          "endDate": "2026-05-28",
          "headcount": 4,
          "coverImageUrl": "https://...",
          "visibility": "PUBLIC",
          "author": {
            "userId": 4,
            "nickname": "여행왕",
            "profileImageUrl": "https://..."
          },
          "likeCount": 12,
          "commentCount": 3,
          "isLiked": false,
          "createdAt": "2026-05-11T18:35:59"
        }
      ],
      "totalElements": 5,
      "totalPages": 1,
      "number": 0,
      "size": 12,
      "first": true,
      "last": true
    }
  }
}
```

> `isLiked`, `isFollowing`은 비로그인 시 각각 `null`, `false`를 반환합니다.

---

## 5. 워크스페이스 검색

### 공개 워크스페이스 검색

`PUBLIC` 워크스페이스만 검색됩니다. `countryCode`, `keyword` 둘 다 생략 가능합니다.

```
GET /api/sns/workspaces/search?countryCode=JP&keyword=도쿄&page=0&size=20
Authorization: Bearer <JWT>  (선택)
```

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `countryCode` | N | ISO 3166-1 alpha-2 (예: `JP`, `FR`). 대소문자 무관 |
| `keyword` | N | 목적지 또는 제목으로 검색 (부분 일치) |
| `page` | N | 기본값 `0` |
| `size` | N | 기본값 `20` |

**Response `200 OK`**

```json
{
  "success": true,
  "data": {
    "content": [ /* PublicWorkspaceResponse[] */ ],
    "totalElements": 10,
    "totalPages": 1,
    "number": 0,
    "size": 20,
    "first": true,
    "last": true
  }
}
```

---

### 워크스페이스 최신 일정 조회

피드/검색 결과에서 워크스페이스를 클릭했을 때, 해당 워크스페이스의 최신 일정을 on-demand로 불러옵니다.

```
GET /api/sns/workspaces/{workspaceId}/latest-schedule
Authorization: Bearer <JWT>  (선택)
```

**Response `200 OK`**

```json
{
  "success": true,
  "data": {
    "version": 3,
    "itemsByDay": {
      "1": [
        {
          "day": 1,
          "orderIndex": 0,
          "category": "ATTRACTION",
          "name": "도쿄타워",
          "address": "도쿄도 미나토구",
          "visitTime": "10:00:00",
          "memo": "전망대 예약 필수"
        }
      ],
      "2": [
        {
          "day": 2,
          "orderIndex": 0,
          "category": "RESTAURANT",
          "name": "스시 오마카세",
          "address": "신주쿠구",
          "visitTime": "12:30:00",
          "memo": null
        }
      ]
    }
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `version` | `number` | 일정 버전 번호 |
| `itemsByDay` | `Record<string, ScheduleItemSummary[]>` | 일차(1부터 시작)를 키로 한 일정 항목 맵 |

> `PRIVATE` 워크스페이스에 대한 조회는 `403` 에러를 반환합니다.

---

## 6. 좋아요

### 좋아요 추가

```
POST /api/sns/workspaces/{workspaceId}/likes
Authorization: Bearer <JWT>  (필수)
```

**Response `200 OK`**
```json
{ "success": true, "data": null }
```

- `FOLLOWERS_ONLY` 워크스페이스는 팔로워만 좋아요 가능합니다.
- `PRIVATE` 워크스페이스는 불가합니다.

---

### 좋아요 취소

```
DELETE /api/sns/workspaces/{workspaceId}/likes
Authorization: Bearer <JWT>  (필수)
```

**Response `204 No Content`**

---

## 7. 댓글

### 댓글 목록 조회

```
GET /api/sns/workspaces/{workspaceId}/comments?page=0&size=20
Authorization: Bearer <JWT>  (선택)
```

**Query Parameters**

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `page` | `0` | 페이지 번호 |
| `size` | `20` | 페이지 크기 |
| `sort` | `createdAt,ASC` | 오래된 순 정렬 |

**Response `200 OK`**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "authorId": 4,
        "authorNickname": "여행왕",
        "authorProfileImageUrl": "https://...",
        "content": "저도 가고 싶어요!",
        "createdAt": "2026-05-26T10:30:00",
        "updatedAt": "2026-05-26T10:30:00"
      }
    ],
    "totalElements": 3,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

> `FOLLOWERS_ONLY` 워크스페이스의 댓글은 팔로워만 조회 가능합니다.  
> `PRIVATE` 워크스페이스의 댓글은 `403`을 반환합니다.

---

### 댓글 작성

```
POST /api/sns/workspaces/{workspaceId}/comments
Authorization: Bearer <JWT>  (필수)
Content-Type: application/json
```

**Request Body**

```json
{
  "content": "저도 가고 싶어요!"
}
```

| 필드 | 제약 | 설명 |
|---|---|---|
| `content` | 필수, 최대 500자 | 댓글 내용 |

**Response `201 Created`**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "authorId": 4,
    "authorNickname": "여행왕",
    "authorProfileImageUrl": "https://...",
    "content": "저도 가고 싶어요!",
    "createdAt": "2026-05-26T10:30:00",
    "updatedAt": "2026-05-26T10:30:00"
  }
}
```

---

### 댓글 수정

본인 댓글만 수정 가능합니다.

```
PATCH /api/sns/workspaces/{workspaceId}/comments/{commentId}
Authorization: Bearer <JWT>  (필수)
Content-Type: application/json
```

**Request Body**

```json
{
  "content": "수정된 댓글입니다."
}
```

**Response `200 OK`** — 수정된 `CommentResponse` 반환

---

### 댓글 삭제

본인 댓글만 삭제 가능합니다.

```
DELETE /api/sns/workspaces/{workspaceId}/comments/{commentId}
Authorization: Bearer <JWT>  (필수)
```

**Response `204 No Content`**

---

## 8. 타입 정의

### TypeScript 타입 정의

```typescript
type WorkspaceVisibility = 'PUBLIC' | 'FOLLOWERS_ONLY' | 'PRIVATE';

type ScheduleCategory =
  | 'ACCOMMODATION'  // 숙박
  | 'RESTAURANT'     // 음식점
  | 'CAFE'           // 카페
  | 'ATTRACTION'     // 관광지
  | 'TRANSPORT';     // 교통

interface AuthorInfo {
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
}

interface PublicWorkspaceResponse {
  id: number;
  title: string;
  destination: string;
  countryCode: string;
  startDate: string;       // YYYY-MM-DD
  endDate: string;         // YYYY-MM-DD
  headcount: number | null;
  coverImageUrl: string | null;
  visibility: WorkspaceVisibility;
  author: AuthorInfo;
  likeCount: number;
  commentCount: number;
  isLiked: boolean | null; // 비로그인 시 null
  createdAt: string;       // ISO 8601
}

interface FollowStatsResponse {
  followerCount: number;
  followingCount: number;
  isFollowing: boolean;
}

interface PublicUserProfileResponse {
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
  followerCount: number;
  followingCount: number;
  isFollowing: boolean;
  publicWorkspaces: PageResponse<PublicWorkspaceResponse>;
}

interface ScheduleItemSummary {
  day: number;
  orderIndex: number;
  category: ScheduleCategory;
  name: string;
  address: string | null;
  visitTime: string | null; // HH:mm:ss
  memo: string | null;
}

interface ScheduleSummary {
  version: number;
  itemsByDay: Record<string, ScheduleItemSummary[]>;
}

interface CommentResponse {
  id: number;
  authorId: number;
  authorNickname: string;
  authorProfileImageUrl: string | null;
  content: string;
  createdAt: string;
  updatedAt: string;
}

// 공통 페이징 타입
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;    // 현재 페이지 (0-based)
  size: number;
  first: boolean;
  last: boolean;
}
```

---

## 9. 에러 코드

| HTTP | 코드 | 메시지 | 발생 상황 |
|---|---|---|---|
| `400` | `SNS_001` | 자기 자신을 팔로우할 수 없습니다 | 본인 팔로우 시도 |
| `403` | `SNS_006` | 공개된 워크스페이스가 아닙니다 | PRIVATE 워크스페이스 접근 또는 FOLLOWERS_ONLY를 팔로워가 아닌 사람이 접근 |
| `403` | `SNS_008` | 댓글 수정/삭제 권한이 없습니다 | 타인 댓글 수정/삭제 시도 |
| `404` | `SNS_003` | 팔로우 관계를 찾을 수 없습니다 | 팔로우 안 한 상태에서 언팔로우 시도 |
| `404` | `SNS_005` | 좋아요를 찾을 수 없습니다 | 좋아요 안 한 상태에서 취소 시도 |
| `404` | `SNS_007` | 댓글을 찾을 수 없습니다 | 존재하지 않는 댓글 수정/삭제 시도 |
| `409` | `SNS_002` | 이미 팔로우 중입니다 | 중복 팔로우 시도 |
| `409` | `SNS_004` | 이미 좋아요를 눌렀습니다 | 중복 좋아요 시도 |

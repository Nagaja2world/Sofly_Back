# SNS 카드 API — 프론트엔드 연동 가이드

> 대상: 프론트엔드 개발자  
> Base URL: `https://api.sofly.co.kr`  
> 모든 API는 `Authorization: Bearer <JWT>` 헤더가 필요합니다.

---

## 목차

1. [개요 및 주요 변경 사항](#1-개요-및-주요-변경-사항)
2. [SNS 카드 CRUD](#2-sns-카드-crud)
3. [피드 API 변경사항](#3-피드-api-변경사항)
4. [공개범위별 접근 가능한 API](#4-공개범위별-접근-가능한-api)
5. [공개범위 동작 정리](#5-공개범위-동작-정리)
6. [타입 정의](#6-타입-정의)
7. [에러 코드](#7-에러-코드)

---

## 1. 개요 및 주요 변경 사항

이번 업데이트에서 추가·변경된 사항은 다음 세 가지입니다.

| 구분 | 내용 |
|---|---|
| **SNS 카드 CRUD 신규** | 워크스페이스당 1개의 SNS 카드(사진+텍스트)를 생성·조회·수정·삭제 |
| **피드 응답 필드 추가** | `GET /api/sns/feed` 응답에 `snsPostId`, `snsFirstImageUrl` 추가 |
| **공개범위 접근 제어 강화** | 일정·여행기·항공편 조회 API가 워크스페이스 공개범위를 따르도록 변경 |

---

## 2. SNS 카드 CRUD

워크스페이스 하나당 SNS 카드는 **1개**만 존재할 수 있습니다.  
SNS 카드의 공개범위(`visibility`)는 워크스페이스의 공개범위와 **별개**입니다.

### 2-1. SNS 카드 생성

```
POST /api/workspaces/{workspaceId}/sns/post
Authorization: Bearer <JWT>
Content-Type: multipart/form-data
```

**Request (form-data)**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `files` | `File[]` | N | 이미지 파일 (최대 10장, 장당 10MB, jpeg/png/webp/heic) |
| `content` | `string` | N | 게시글 텍스트 |
| `visibility` | `string` | **Y** | `PUBLIC` / `FOLLOWERS_ONLY` / `PRIVATE` |

**Response `201 Created`**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "workspaceId": 6,
    "author": {
      "userId": 4,
      "nickname": "여행왕",
      "profileImageUrl": "https://..."
    },
    "content": "도쿄 3박 4일 후기 🗼",
    "visibility": "PUBLIC",
    "images": [
      { "id": 1, "url": "https://...", "orderIndex": 0 },
      { "id": 2, "url": "https://...", "orderIndex": 1 }
    ],
    "createdAt": "2026-06-01T10:00:00"
  }
}
```

> 해당 워크스페이스에 이미 SNS 카드가 있으면 `409 SNS_011` 에러를 반환합니다.

---

### 2-2. SNS 카드 조회 (이미지 전체)

인스타그램처럼 피드에서 카드를 탭했을 때 전체 이미지를 불러오는 엔드포인트입니다.

```
GET /api/workspaces/{workspaceId}/sns/post
Authorization: Bearer <JWT>
```

**접근 권한**

| SNS 카드 공개범위 | 접근 가능 대상 |
|---|---|
| `PUBLIC` | 모든 로그인 사용자 |
| `FOLLOWERS_ONLY` | 작성자 본인 + 팔로워 + 워크스페이스 멤버 |
| `PRIVATE` | 작성자 본인만 |

**Response `200 OK`** — 위 `SnsPostResponse` 형식과 동일 (이미지 전체 포함)

---

### 2-3. SNS 카드 수정

```
PATCH /api/workspaces/{workspaceId}/sns/post
Authorization: Bearer <JWT>
Content-Type: multipart/form-data
```

**Request (form-data)**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `files` | `File[]` | N | 새 이미지 전달 시 **기존 이미지 전체 교체** |
| `content` | `string` | N | 수정할 텍스트 (미전달 시 기존 값 유지) |
| `visibility` | `string` | N | 수정할 공개범위 (미전달 시 기존 값 유지) |

> 이미지 일부만 수정하는 기능은 없습니다. `files`를 전달하면 기존 이미지가 **전부 삭제**되고 새 이미지로 교체됩니다.

**Response `200 OK`** — 수정된 `SnsPostResponse` 반환

---

### 2-4. SNS 카드 삭제

작성자 본인만 삭제할 수 있습니다. S3 이미지도 함께 삭제됩니다.

```
DELETE /api/workspaces/{workspaceId}/sns/post
Authorization: Bearer <JWT>
```

**Response `204 No Content`**

---

## 3. 피드 API 변경사항

### 변경된 응답 필드

`GET /api/sns/feed` 응답의 각 워크스페이스 객체에 두 필드가 **추가**되었습니다.

```json
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
  "author": { ... },
  "likeCount": 12,
  "commentCount": 3,
  "isLiked": false,
  "createdAt": "2026-05-11T18:35:59",

  "snsPostId": 1,
  "snsFirstImageUrl": "https://s3.amazonaws.com/..."
}
```

| 신규 필드 | 타입 | 설명 |
|---|---|---|
| `snsPostId` | `number \| null` | SNS 카드 ID. 카드가 없으면 `null` |
| `snsFirstImageUrl` | `string \| null` | SNS 카드 첫 번째 이미지 URL. 없으면 `null` |

**프론트 처리 가이드**
- `snsPostId`가 `null`이면 SNS 카드가 없는 워크스페이스 → 기존 `coverImageUrl` 사용
- `snsPostId`가 있으면 SNS 카드 존재 → 썸네일로 `snsFirstImageUrl` 표시
- 카드 클릭 시 `GET /api/workspaces/{workspaceId}/sns/post` 호출로 전체 이미지 로드

### FOLLOWERS_ONLY 워크스페이스 피드 노출

이전에는 피드에 `PUBLIC` 워크스페이스만 노출되었습니다.  
이제 **내가 팔로우하는 사람의 `FOLLOWERS_ONLY` 워크스페이스도 피드에 표시**됩니다.

| 워크스페이스 공개범위 | 피드 노출 |
|---|---|
| `PUBLIC` | 모든 사용자에게 노출 |
| `FOLLOWERS_ONLY` | 소유자를 팔로우하는 사용자에게만 노출 |
| `PRIVATE` | 피드에 노출되지 않음 |

> SNS 카드의 공개범위가 `PRIVATE`이면 해당 워크스페이스가 피드에 나와도 `snsPostId`/`snsFirstImageUrl`은 `null`로 반환됩니다.

---

## 4. 공개범위별 접근 가능한 API

워크스페이스의 `visibility` 설정에 따라 아래 API들의 접근이 제한됩니다.

### 접근 허용 조건

| 워크스페이스 공개범위 | 접근 가능 대상 |
|---|---|
| `PUBLIC` | 모든 로그인 사용자 |
| `FOLLOWERS_ONLY` | 워크스페이스 멤버 + 소유자 팔로워 |
| `PRIVATE` | 워크스페이스 멤버만 |

### 적용 API 목록

| API | 설명 |
|---|---|
| `GET /api/workspaces/{workspaceId}/schedules` | 일정 버전 목록 |
| `GET /api/workspaces/{workspaceId}/schedules/latest` | 최신 일정 조회 |
| `GET /api/workspaces/{workspaceId}/travellogs` | 여행기 요약 목록 |
| `GET /api/workspaces/{workspaceId}/travellogs/full` | 여행기 전체 목록 (content·photos 포함) |
| `GET /api/workspaces/{workspaceId}/flights` | 저장된 항공편 목록 |

> 권한 없이 접근하면 `403 WORKSPACE_ACCESS_DENIED` 또는 `403 WORKSPACE_FORBIDDEN`을 반환합니다.

---

## 5. 공개범위 동작 정리

### 워크스페이스 공개범위

| 공개범위 | 피드 노출 | 일정·여행기·항공편 조회 | SNS 카드 생성 |
|---|---|---|---|
| `PUBLIC` | 전체 | 누구나 | 멤버만 |
| `FOLLOWERS_ONLY` | 팔로워만 | 멤버 + 팔로워 | 멤버만 |
| `PRIVATE` | 비노출 | 멤버만 | 멤버만 |

### SNS 카드 공개범위 (워크스페이스와 별개)

| 공개범위 | `GET /sns/post` 조회 | 피드의 `snsFirstImageUrl` 노출 |
|---|---|---|
| `PUBLIC` | 누구나 | 항상 노출 |
| `FOLLOWERS_ONLY` | 작성자 + 팔로워 + 워크스페이스 멤버 | 팔로워에게만 노출 |
| `PRIVATE` | 작성자만 | 노출 안 됨 (`null`) |

---

## 6. 타입 정의

```typescript
type SnsPostVisibility = 'PUBLIC' | 'FOLLOWERS_ONLY' | 'PRIVATE';
type WorkspaceVisibility = 'PUBLIC' | 'FOLLOWERS_ONLY' | 'PRIVATE';

interface SnsPostImageResponse {
  id: number;
  url: string;
  orderIndex: number;
}

// SNS 카드 상세 (이미지 전체) — POST/GET/PATCH 응답
interface SnsPostResponse {
  id: number;
  workspaceId: number;
  author: AuthorInfo;
  content: string | null;
  visibility: SnsPostVisibility;
  images: SnsPostImageResponse[];  // 순서대로 정렬됨 (orderIndex ASC)
  createdAt: string;               // ISO 8601
}

// 피드 워크스페이스 응답 — snsPostId, snsFirstImageUrl 필드 추가됨
interface PublicWorkspaceResponse {
  id: number;
  title: string;
  destination: string;
  countryCode: string;
  startDate: string;              // YYYY-MM-DD
  endDate: string;                // YYYY-MM-DD
  headcount: number | null;
  coverImageUrl: string | null;
  visibility: WorkspaceVisibility;
  author: AuthorInfo;
  likeCount: number;
  commentCount: number;
  isLiked: boolean | null;
  createdAt: string;              // ISO 8601
  snsPostId: number | null;       // SNS 카드 없으면 null
  snsFirstImageUrl: string | null; // SNS 카드 이미지 없으면 null
}

interface AuthorInfo {
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
}
```

---

## 7. 에러 코드

### SNS 카드 에러

| HTTP | 코드 | 설명 | 발생 상황 |
|---|---|---|---|
| `403` | `SNS_010` | SNS 카드 접근 권한 없음 | 공개범위 밖의 사용자가 조회·수정·삭제 시도 |
| `404` | `SNS_009` | SNS 카드를 찾을 수 없음 | 카드가 없는 워크스페이스에 조회·수정·삭제 시도 |
| `409` | `SNS_011` | 이미 SNS 카드가 존재함 | 카드가 이미 있는 워크스페이스에 생성 시도 |

### 파일 업로드 에러

| HTTP | 코드 | 설명 |
|---|---|---|
| `400` | `TOO_MANY_FILES` | 10장 초과 업로드 시도 |
| `400` | `FILE_TOO_LARGE` | 단일 파일 10MB 초과 |
| `400` | `INVALID_FILE_TYPE` | jpeg/png/webp/heic 이외 파일 형식 |

### 워크스페이스 접근 에러

| HTTP | 코드 | 설명 |
|---|---|---|
| `403` | `WORKSPACE_ACCESS_DENIED` | 공개범위 밖의 사용자가 일정·여행기·항공편 조회 시도 |
| `403` | `WORKSPACE_FORBIDDEN` | 멤버가 아닌 사용자가 일정 조회 시도 (ScheduleService) |

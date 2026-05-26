# Messaging API 연동 가이드

> 대상: 프론트엔드 개발자  
> Base URL: `http://localhost:8080` (개발) / `https://api.sofly.kr` (프로덕션)  
> 모든 REST API는 `Authorization: Bearer <JWT>` 헤더가 필요합니다.

---

## 목차

1. [전체 흐름 요약](#1-전체-흐름-요약)
2. [REST API](#2-rest-api)
   - [내 채팅방 목록 조회](#21-내-채팅방-목록-조회)
   - [연락처 목록 조회](#22-연락처-목록-조회)
   - [채팅방 생성](#23-채팅방-생성)
   - [메시지 히스토리 조회](#24-메시지-히스토리-조회)
3. [WebSocket (실시간 채팅)](#3-websocket-실시간-채팅)
   - [연결](#31-연결)
   - [인증](#32-인증)
   - [메시지 구독 (수신)](#33-메시지-구독-수신)
   - [메시지 발행 (송신)](#34-메시지-발행-송신)
4. [타입 정의](#4-타입-정의)
5. [에러 코드](#5-에러-코드)
6. [연동 예제 코드](#6-연동-예제-코드)

---

## 1. 전체 흐름 요약

```
[채팅 화면 진입]
    │
    ├─ GET /api/v1/messaging/rooms         ← 내 채팅방 목록 로드
    │
    ├─ [채팅방 선택]
    │     ├─ GET /api/v1/messaging/rooms/{roomId}/messages   ← 이전 메시지 히스토리 로드
    │     └─ WebSocket 연결 후 /sub/chat/{roomId} 구독       ← 실시간 수신 시작
    │
    ├─ [새 채팅 시작]
    │     ├─ GET /api/v1/messaging/contacts                  ← 연락처 목록 로드
    │     └─ POST /api/v1/messaging/rooms                    ← 채팅방 생성
    │
    └─ [메시지 전송]
          └─ WebSocket SEND /pub/chat.message.{roomId}       ← 실시간 송신
```

> **메시지 저장소:** MongoDB  
> **실시간 브로커:** Redis Pub/Sub → STOMP SimpleBroker  
> **인증 방식:** JWT (REST는 HTTP 헤더, WebSocket은 STOMP 헤더)

---

## 2. REST API

### 2.1 내 채팅방 목록 조회

내가 멤버로 등록된 모든 채팅방 목록을 반환합니다.

```
GET /api/v1/messaging/rooms
Authorization: Bearer <JWT>
```

**Response `200 OK`**

```json
{
  "success": true,
  "data": [
    {
      "roomId": 1,
      "type": "DIRECT",
      "name": "홍길동",
      "workspaceId": null
    },
    {
      "roomId": 2,
      "type": "WORKSPACE",
      "name": "제주도 여행팀",
      "workspaceId": 10
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `roomId` | `number` | 채팅방 ID (WebSocket 구독 시 사용) |
| `type` | `"DIRECT" \| "GROUP" \| "WORKSPACE"` | 채팅방 종류 |
| `name` | `string \| null` | 채팅방 이름 (1:1은 상대방 닉네임 권장) |
| `workspaceId` | `number \| null` | WORKSPACE 타입일 때만 값 존재 |

---

### 2.2 연락처 목록 조회

1:1 채팅 가능한 유저 목록입니다. 내가 속한 모든 워크스페이스의 멤버를 반환합니다 (본인 제외, 중복 제거).

```
GET /api/v1/messaging/contacts
Authorization: Bearer <JWT>
```

**Response `200 OK`**

```json
{
  "success": true,
  "data": [
    {
      "userId": 42,
      "nickname": "홍길동",
      "profileImageUrl": "https://example.com/profile/42.jpg",
      "email": "hong@example.com"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `userId` | `number` | 채팅방 생성 시 `memberIds`에 사용 |
| `nickname` | `string` | 표시 이름 |
| `profileImageUrl` | `string \| null` | 프로필 이미지 URL |
| `email` | `string` | 이메일 |

---

### 2.3 채팅방 생성

```
POST /api/v1/messaging/rooms
Authorization: Bearer <JWT>
Content-Type: application/json
```

**Request Body**

```json
{
  "type": "DIRECT",
  "name": "홍길동",
  "workspaceId": null,
  "memberIds": [1, 42]
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `type` | `"DIRECT" \| "GROUP" \| "WORKSPACE"` | Y | 채팅방 종류 |
| `name` | `string \| null` | N | 채팅방 이름 (GROUP/WORKSPACE 권장) |
| `workspaceId` | `number \| null` | N | WORKSPACE 타입일 때 워크스페이스 ID |
| `memberIds` | `number[]` | Y | 초대할 유저 ID 목록 (본인 포함 권장) |

> **주의:** `memberIds`에 **본인 ID를 포함**하지 않으면 생성한 채팅방에 본인이 멤버로 등록되지 않습니다.

**채팅방 타입별 생성 가이드**

| 타입 | `name` | `workspaceId` | `memberIds` |
|---|---|---|---|
| `DIRECT` | 상대방 닉네임 (옵션) | `null` | `[내 ID, 상대방 ID]` |
| `GROUP` | 그룹 이름 | `null` | `[내 ID, 멤버1, 멤버2, ...]` |
| `WORKSPACE` | 워크스페이스 이름 | 워크스페이스 ID | 워크스페이스 멤버 ID 목록 |

**Response `200 OK`**

```json
{
  "success": true,
  "data": {
    "id": 5,
    "type": "DIRECT",
    "name": "홍길동",
    "workspaceId": null,
    "createdAt": "2026-05-26T10:30:00",
    "updatedAt": "2026-05-26T10:30:00"
  }
}
```

생성된 `id` 값을 `roomId`로 사용하여 WebSocket에 구독합니다.

---

### 2.4 메시지 히스토리 조회

채팅방 입장 시 이전 메시지를 불러옵니다. 최신 메시지부터 내림차순으로 반환됩니다.

```
GET /api/v1/messaging/rooms/{roomId}/messages?page=0&size=30
Authorization: Bearer <JWT>
```

| 쿼리 파라미터 | 기본값 | 설명 |
|---|---|---|
| `page` | `0` | 페이지 번호 (0-based) |
| `size` | `30` | 페이지 당 메시지 수 |

**Response `200 OK`**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "664f1a2b3c4d5e6f7a8b9c0d",
        "messagingRoomId": 1,
        "senderId": 42,
        "senderNickname": "홍길동",
        "content": "안녕하세요!",
        "type": "TEXT",
        "createdAt": "2026-05-26T10:35:00"
      }
    ],
    "totalElements": 120,
    "totalPages": 4,
    "number": 0,
    "size": 30,
    "first": true,
    "last": false
  }
}
```

> 응답은 **최신순 내림차순**이므로 UI에서 표시 시 배열을 역순(`reverse()`)으로 정렬하세요.

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `string` | MongoDB ObjectId (문자열) |
| `messagingRoomId` | `number` | 채팅방 ID |
| `senderId` | `number` | 발신자 유저 ID |
| `senderNickname` | `string` | 발신자 닉네임 (DB 조회 없이 저장된 값) |
| `content` | `string` | 메시지 내용 |
| `type` | `"TEXT" \| "IMAGE" \| "FILE"` | 메시지 타입 |
| `createdAt` | `string` (ISO 8601) | 전송 시각 |

---

## 3. WebSocket (실시간 채팅)

STOMP over SockJS 프로토콜을 사용합니다.

### 3.1 연결

| 항목 | 값 |
|---|---|
| WebSocket 엔드포인트 | `ws://localhost:8080/ws` |
| SockJS 엔드포인트 | `http://localhost:8080/ws` |
| 프로토콜 | STOMP over SockJS |

> SockJS는 WebSocket을 지원하지 않는 환경에서 자동으로 HTTP long-polling 등으로 fallback됩니다. **SockJS 방식 사용을 권장합니다.**

---

### 3.2 인증

WebSocket 연결 시(`CONNECT` 프레임) STOMP 헤더에 JWT를 포함해야 합니다.  
이후 `SEND`, `SUBSCRIBE` 프레임에도 동일하게 헤더를 포함해야 합니다.

```
CONNECT
Authorization: Bearer <JWT>
```

인증 실패 시 메시지 전송이 무시됩니다 (서버 경고 로그 발생).

---

### 3.3 메시지 구독 (수신)

채팅방에 입장하면 해당 채팅방 토픽을 구독합니다.

```
SUBSCRIBE
destination: /sub/chat/{roomId}
```

구독 후 다른 유저가 메시지를 전송하면 아래 형태의 JSON이 수신됩니다.

**수신 메시지 형태**

```json
{
  "id": "664f1a2b3c4d5e6f7a8b9c0d",
  "messagingRoomId": 1,
  "senderId": 42,
  "senderNickname": "홍길동",
  "content": "안녕하세요!",
  "type": "TEXT",
  "createdAt": "2026-05-26T10:35:00"
}
```

> 본인이 전송한 메시지도 서버를 거쳐 구독 채널로 브로드캐스트됩니다.  
> 따라서 **낙관적 UI 업데이트 후 서버 응답으로 교체하거나, 중복 방지 처리**를 권장합니다.

---

### 3.4 메시지 발행 (송신)

```
SEND
destination: /pub/chat.message.{roomId}
Authorization: Bearer <JWT>

{
  "content": "안녕하세요!",
  "type": "TEXT"
}
```

**Request 필드**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `content` | `string` | Y | 메시지 내용 |
| `type` | `"TEXT" \| "IMAGE" \| "FILE"` | Y | 메시지 타입 |

> `senderId`, `senderNickname`은 서버에서 JWT 토큰과 WebSocket 세션에서 자동으로 추출합니다. **클라이언트에서 보내지 않아도 됩니다.**

---

## 4. 타입 정의

### ChatRoomType

| 값 | 설명 |
|---|---|
| `DIRECT` | 1:1 채팅 |
| `GROUP` | 초대 기반 그룹 채팅 |
| `WORKSPACE` | 워크스페이스 연동 채팅 |

### ChatMessageType

| 값 | 설명 |
|---|---|
| `TEXT` | 텍스트 메시지 |
| `IMAGE` | 이미지 메시지 (현재 content에 URL 저장) |
| `FILE` | 파일 메시지 (현재 content에 URL 저장) |

### TypeScript 타입 정의 (참고용)

```typescript
type ChatRoomType = 'DIRECT' | 'GROUP' | 'WORKSPACE';
type ChatMessageType = 'TEXT' | 'IMAGE' | 'FILE';

interface MessagingRoomSummary {
  roomId: number;
  type: ChatRoomType;
  name: string | null;
  workspaceId: number | null;
}

interface MessagingContact {
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
  email: string;
}

interface MessagingRoomCreateRequest {
  type: ChatRoomType;
  name?: string;
  workspaceId?: number;
  memberIds: number[];
}

interface MessagingMessageResponse {
  id: string;           // MongoDB ObjectId
  messagingRoomId: number;
  senderId: number;
  senderNickname: string;
  content: string;
  type: ChatMessageType;
  createdAt: string;    // ISO 8601
}

interface MessagingMessageRequest {
  content: string;
  type: ChatMessageType;
}
```

---

## 5. 에러 코드

| HTTP | 설명 | 원인 |
|---|---|---|
| `401` | 인증 정보 없음 | JWT 없음 또는 만료 |
| `403` | 접근 거부 | 채팅방 멤버가 아닌 유저가 메시지 전송 시도 |
| `404` | 채팅방을 찾을 수 없음 | 존재하지 않는 `roomId` |

---

## 6. 연동 예제 코드

아래는 `@stomp/stompjs` + `sockjs-client` 기반 예제입니다.

**패키지 설치**

```bash
npm install @stomp/stompjs sockjs-client
npm install -D @types/sockjs-client
```

**연결 및 메시지 송수신**

```typescript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BASE_URL = 'http://localhost:8080';

function createChatClient(token: string, roomId: number) {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${BASE_URL}/ws`),

    // CONNECT 프레임 헤더에 JWT 포함
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },

    onConnect: () => {
      console.log('WebSocket 연결 성공');

      // 채팅방 구독
      client.subscribe(
        `/sub/chat/${roomId}`,
        (frame) => {
          const message: MessagingMessageResponse = JSON.parse(frame.body);
          console.log('수신:', message);
          // UI 업데이트 로직
        },
        // SUBSCRIBE 프레임에도 Authorization 헤더 포함
        { Authorization: `Bearer ${token}` }
      );
    },

    onDisconnect: () => {
      console.log('WebSocket 연결 해제');
    },

    onStompError: (frame) => {
      console.error('STOMP 에러:', frame);
    },
  });

  client.activate();
  return client;
}

// 메시지 전송
function sendMessage(
  client: Client,
  token: string,
  roomId: number,
  content: string
) {
  client.publish({
    destination: `/pub/chat.message.${roomId}`,
    headers: { Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      content,
      type: 'TEXT',
    } satisfies MessagingMessageRequest),
  });
}

// 채팅방 퇴장 시 연결 해제
function disconnect(client: Client) {
  client.deactivate();
}
```

**메시지 히스토리 로드 (REST)**

```typescript
async function loadHistory(
  token: string,
  roomId: number,
  page = 0,
  size = 30
): Promise<MessagingMessageResponse[]> {
  const res = await fetch(
    `${BASE_URL}/api/v1/messaging/rooms/${roomId}/messages?page=${page}&size=${size}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const json = await res.json();
  // 최신순 내림차순이므로 시간순 정렬
  return (json.data.content as MessagingMessageResponse[]).reverse();
}
```

**채팅 화면 진입 시 초기화 패턴**

```typescript
async function enterChatRoom(token: string, roomId: number) {
  // 1. 이전 메시지 히스토리 로드
  const history = await loadHistory(token, roomId);

  // 2. WebSocket 연결 및 구독
  const client = createChatClient(token, roomId);

  return { history, client };
}
```

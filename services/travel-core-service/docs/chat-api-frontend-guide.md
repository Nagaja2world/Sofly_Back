# AI Chat 프론트엔드 연동 가이드

> 대상 컨트롤러: `ChatController`, `ChatMessageController`  
> Base URL: `http://localhost:8080/api/v1/chat`  
> 인증: 모든 요청에 `Authorization: Bearer <JWT>` 헤더 필요

---

## 1. 전체 흐름 개요

```
워크스페이스 진입
    │
    ▼
[채팅방 목록 조회] ──── 없으면 ────► [채팅방 생성]
    │
    ▼
[채팅방 선택 → 메시지 히스토리 로드]
    │
    ▼
[메시지 전송] ──── 일반 응답 or SSE 스트리밍 선택
    │
    ▼
AI가 "확정해줘" / "저장해줘" 응답 감지
    │
    ▼
[일정 저장 API 호출] → Schedule 생성
```

---

## 2. API 명세

### 2-1. 채팅방 생성

```
POST /api/v1/chat/rooms
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body**
```json
{
  "workspaceId": 1,
  "title": "도쿄 여행 계획"   // optional, 없으면 "새 여행 계획"
}
```

**Response 200**
```json
{
  "roomId": 42,
  "title": "도쿄 여행 계획",
  "lastMessage": null
}
```

---

### 2-2. 채팅방 목록 조회

```
GET /api/v1/chat/workspaces/{workspaceId}/rooms
Authorization: Bearer <JWT>
```

**Response 200**
```json
[
  {
    "roomId": 42,
    "title": "도쿄 여행 계획",
    "lastMessage": "3박 4일 일정을 정리해 드릴게요…"
  },
  {
    "roomId": 43,
    "title": "오사카 여행",
    "lastMessage": null
  }
]
```

> `lastMessage`는 AI 마지막 답변의 앞 100자 미리보기입니다.

---

### 2-3. 메시지 히스토리 조회

채팅방에 진입할 때 기존 대화 내역을 불러옵니다.

```
GET /api/v1/chat/rooms/{roomId}/messages
Authorization: Bearer <JWT>
```

**Response 200**
```json
{
  "roomId": 42,
  "messages": [
    {
      "content": "도쿄 3박 4일 일정 짜줘",
      "role": "USER",
      "createdAt": "2026-05-12T10:00:00"
    },
    {
      "content": "네! 여행 인원이 몇 명인가요?",
      "role": "ASSISTANT",
      "createdAt": "2026-05-12T10:00:05"
    }
  ]
}
```

> `role` 값: `USER` | `ASSISTANT`

---

### 2-4. 메시지 전송 (일반, 비스트리밍)

```
POST /api/v1/chat/rooms/{roomId}
Content-Type: application/json
Authorization: Bearer <JWT>
```

**Request Body**
```json
{
  "message": "2박 3일, 2명이야"
}
```

**Response 200**
```json
{
  "roomId": 42,
  "message": "좋아요! 예산은 어느 정도 생각하고 계신가요?",
  "role": "ASSISTANT",
  "createdAt": "2026-05-12T10:01:00"
}
```

> 응답이 완성된 후 한 번에 오므로 짧은 답변에 적합합니다.  
> 긴 일정 생성 시 체감 속도가 느릴 수 있으므로 스트리밍을 권장합니다.

---

### 2-5. 메시지 전송 (SSE 스트리밍) — 권장

```
POST /api/v1/chat/rooms/{roomId}/stream
Content-Type: application/json
Accept: text/event-stream
Authorization: Bearer <JWT>
```

**Request Body**
```json
{
  "message": "확정해줘"
}
```

**SSE 이벤트 형식**

| event 이름 | 의미 | data 내용 |
|---|---|---|
| (없음, 기본) | 텍스트 청크 | AI 응답 일부 문자열 |
| `error` | 에러 발생 | 에러 메시지 문자열 |
| `: keep-alive` | 연결 유지 heartbeat | 없음 (무시) |
| 스트림 종료 | 응답 완료 | EventSource가 `close` 이벤트 발생 |

**예시 SSE 수신 흐름**
```
data: 네

data: , 아래와

data:  같이 일정을 확정했습니다.

data: {"days":[{"day":1,"items":[...]}]}

: keep-alive

[스트림 종료]
```

---

### 2-6. AI 확정 일정 저장

AI 응답에서 사용자가 일정 확정 의사를 밝힌 후 호출합니다.

```
POST /api/v1/chat/rooms/{roomId}/save-schedule?workspaceId={workspaceId}
Authorization: Bearer <JWT>
```

**Response 200** — 생성된 Schedule 객체 (ScheduleResponse 형식)

---

## 3. 시나리오별 구현 가이드

### 시나리오 A: 채팅방 사이드바 (좌측 탭)

1. 워크스페이스 진입 시 `GET /workspaces/{workspaceId}/rooms` 호출
2. 목록을 `roomId`, `title`, `lastMessage`(미리보기)로 렌더링
3. "+" 버튼 클릭 → `POST /rooms` 호출 → 생성된 방을 목록 최상단에 추가
4. 방 클릭 → 시나리오 B로 이동

---

### 시나리오 B: 채팅방 진입 및 대화

1. `GET /rooms/{roomId}/messages`로 히스토리 불러와 화면에 렌더링
2. `role === "USER"` → 오른쪽 버블, `role === "ASSISTANT"` → 왼쪽 버블
3. 전송 버튼 클릭 → 시나리오 C(스트리밍)으로 전송

---

### 시나리오 C: SSE 스트리밍 메시지 전송

SSE는 브라우저 `EventSource`가 `GET`만 지원하므로 **`fetch` + `ReadableStream`** 방식으로 구현합니다.

```typescript
async function sendMessageStream(roomId: number, message: string, onChunk: (text: string) => void) {
  const response = await fetch(`/api/v1/chat/rooms/${roomId}/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getJwt()}`,
      'Accept': 'text/event-stream',
    },
    body: JSON.stringify({ message }),
  });

  const reader = response.body!.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });

    // SSE는 빈 줄로 이벤트를 구분
    const lines = buffer.split('\n');
    buffer = lines.pop() ?? '';

    for (const line of lines) {
      if (line.startsWith('data:')) {
        const chunk = line.slice(5).trim();
        if (chunk) onChunk(chunk);
      }
      if (line.startsWith('event: error')) {
        // 다음 data 라인이 에러 메시지
      }
      // ': keep-alive' 라인은 무시
    }
  }
}
```

**UI 처리 패턴**
```typescript
// 사용 예시
let aiText = '';
await sendMessageStream(roomId, userMessage, (chunk) => {
  aiText += chunk;
  setStreamingMessage(aiText); // 실시간 렌더링
});
// 스트림 완료 후 확정 버튼 활성화 여부 판단
checkIfScheduleReady(aiText);
```

---

### 시나리오 D: AI 일정 확정 감지 및 저장

AI는 3단계 대화를 거칩니다:
1. **정보 수집** — 목적지, 인원, 기간, 예산 등을 질문
2. **자연어 제안** — 일정을 텍스트로 설명
3. **JSON 확정 출력** — 사용자가 "확정해줘", "저장해줘" 등을 입력하면 JSON 반환

**감지 방법**: AI 응답에 `"days":[` 패턴이 포함되면 JSON 확정 응답입니다.

```typescript
function isScheduleConfirmed(aiResponse: string): boolean {
  return aiResponse.includes('"days"') && aiResponse.includes('"items"');
}

// 스트림 완료 후
if (isScheduleConfirmed(fullResponse)) {
  showSaveScheduleButton(); // "일정 저장하기" 버튼 표시
}
```

**저장 버튼 클릭 시**
```typescript
async function saveSchedule(roomId: number, workspaceId: number) {
  const res = await fetch(
    `/api/v1/chat/rooms/${roomId}/save-schedule?workspaceId=${workspaceId}`,
    {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${getJwt()}` },
    }
  );
  const schedule = await res.json();
  // schedule 페이지로 이동하거나 모달 표시
}
```

---

## 4. 에러 처리

| HTTP 상태 | 상황 | 처리 방법 |
|---|---|---|
| 401 | JWT 만료 | 토큰 갱신 후 재시도 |
| 404 | roomId / workspaceId 없음 | 목록으로 돌아가기 |
| 400 | `message` 빈 문자열 전송 | 입력 validation |
| SSE `event: error` | AI 처리 중 오류 | 에러 메시지 버블 표시 |

---

## 5. 전체 상태 흐름 (React 기준 예시)

```
[WorkspacePage]
  └─ useEffect → GET /workspaces/{id}/rooms
  └─ state: rooms[]

[ChatSidebar]
  └─ rooms[] 렌더링
  └─ 방 클릭 → navigate to /chat/{roomId}
  └─ + 버튼 → POST /rooms → rooms에 추가

[ChatRoomPage]
  └─ useEffect → GET /rooms/{roomId}/messages → messages[]
  └─ 전송 → sendMessageStream()
       └─ 낙관적 UI: USER 메시지 즉시 추가
       └─ 스트리밍: ASSISTANT 버블에 실시간 append
       └─ 완료: isScheduleConfirmed() 체크
            └─ true → "일정 저장" 버튼 표시
                  └─ 클릭 → POST /save-schedule → 성공 toast + 일정 페이지 이동
```

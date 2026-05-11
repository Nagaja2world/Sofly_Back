# 워크스페이스 초대 시스템

상대방의 동의를 받는 2단계 초대 흐름입니다.  
PostgreSQL(영속성) + Redis(TTL 만료) + Kafka(알림 이벤트)를 조합합니다.

---

## 전체 흐름

```
[초대하는 사람 - OWNER]
  POST /api/workspaces/{workspaceId}/members/invite
  { "userId": 123 }
        │
        ├─ DB: workspace_invitations INSERT (status=PENDING, expires_at=+7일)
        ├─ Redis: SET invite:{invitationId} "1" EX 604800
        └─ Kafka: workspace.invitation 토픽 발행
                        │
               [NotificationConsumer]
                        │
                  log (stub) — 추후 FCM/웹소켓 연결

[초대받은 사람]
  GET  /api/invitations              → PENDING 목록 확인
  POST /api/invitations/{id}/accept  → 수락 (WorkspaceMember VIEWER로 추가)
  POST /api/invitations/{id}/reject  → 거절
```

---

## API 명세

### 1. 이메일로 유저 검색 (자동완성)

```
GET /api/users/search?email={prefix}
Authorization: Bearer {token}
```

**Response**
```json
[
  {
    "id": 1,
    "email": "sm010422@naver.com",
    "nickname": "박상민",
    "profileImageUrl": "https://..."
  }
]
```

- 입력한 prefix로 시작하는 이메일 유저 최대 10명 반환
- 프론트에서 드롭다운 렌더링 후 선택 시 `id` 값 보관

---

### 2. 초대 요청 생성

```
POST /api/workspaces/{workspaceId}/members/invite
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": 123
}
```

> 워크스페이스 OWNER만 가능

**Response**
```json
{
  "invitationId": 42,
  "workspaceId": 7,
  "workspaceTitle": "도쿄 여행",
  "inviterNickname": "박상민",
  "inviterEmail": "sm@naver.com",
  "status": "PENDING",
  "expiresAt": "2026-05-19T15:00:00",
  "createdAt": "2026-05-12T15:00:00"
}
```

**에러 케이스**

| HTTP | 코드 | 설명 |
|---|---|---|
| 400 | WORKSPACE_015 | 자기 자신 초대 |
| 403 | WORKSPACE_002 | OWNER가 아님 |
| 404 | WORKSPACE_001 | 워크스페이스 없음 |
| 404 | USER_001 | 대상 유저 없음 |
| 409 | WORKSPACE_003 | 이미 멤버 |
| 409 | WORKSPACE_014 | 이미 PENDING 초대 존재 |

---

### 3. 내 초대 목록 조회

```
GET /api/invitations
Authorization: Bearer {token}
```

**Response**
```json
[
  {
    "invitationId": 42,
    "workspaceId": 7,
    "workspaceTitle": "도쿄 여행",
    "inviterNickname": "박상민",
    "inviterEmail": "sm@naver.com",
    "status": "PENDING",
    "expiresAt": "2026-05-19T15:00:00",
    "createdAt": "2026-05-12T15:00:00"
  }
]
```

- `status=PENDING`이고 만료되지 않은 초대만 반환
- 앱 실행 시 또는 알림 클릭 시 이 API를 호출해서 초대 목록 표시

---

### 4. 초대 수락

```
POST /api/invitations/{invitationId}/accept
Authorization: Bearer {token}
```

**Response** — 추가된 WorkspaceMember 정보
```json
{
  "memberId": 15,
  "userId": 123,
  "nickname": "홍길동",
  "userEmail": "hong@naver.com",
  "role": "VIEWER"
}
```

**에러 케이스**

| HTTP | 코드 | 설명 |
|---|---|---|
| 400 | WORKSPACE_013 | Redis 토큰 없음 (만료) |
| 404 | WORKSPACE_011 | 초대 없음 (내 초대가 아님) |
| 409 | WORKSPACE_012 | 이미 수락/거절한 초대 |
| 409 | WORKSPACE_003 | 이미 멤버 |

---

### 5. 초대 거절

```
POST /api/invitations/{invitationId}/reject
Authorization: Bearer {token}
```

**Response** — `null` (200 OK)

---

## 프론트엔드 구현 가이드

### 초대하기 UI (워크스페이스 설정 > 멤버 관리)

```
1. 초대 입력창에 이메일 타이핑
2. 300ms debounce 후 GET /api/users/search?email={입력값} 호출
3. 드롭다운에 결과 렌더링 (email + nickname + 프로필 이미지)
4. 유저 선택 → userId 저장
5. "초대하기" 버튼 클릭 → POST /api/workspaces/{id}/members/invite { userId }
6. 성공 시 "초대 요청을 보냈습니다" 토스트
```

### 초대 알림 UI (홈 또는 알림 센터)

```
1. 앱 진입 시 GET /api/invitations 호출
2. 결과가 있으면 알림 배지 또는 배너 표시
   예: "박상민님이 [도쿄 여행] 워크스페이스에 초대했습니다."
3. 수락 버튼 → POST /api/invitations/{id}/accept
   - 성공 시 해당 워크스페이스로 이동
4. 거절 버튼 → POST /api/invitations/{id}/reject
   - 성공 시 목록에서 제거
```

### 만료 처리

- `expiresAt`이 현재 시각보다 이전이면 프론트에서도 만료 표시
- 서버도 만료 초대는 수락 시 `400 WORKSPACE_013` 반환

---

## 내부 구조

### DB 테이블: `workspace_invitations`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | |
| workspace_id | BIGINT FK | |
| inviter_id | BIGINT FK | 초대한 사람 |
| invitee_id | BIGINT FK | 초대받은 사람 |
| status | VARCHAR | PENDING / ACCEPTED / REJECTED |
| expires_at | TIMESTAMP | 생성 시각 + 7일 |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### Redis 키

```
invite:{invitationId}  →  "1"  (TTL: 7일)
```

수락 또는 거절 시 즉시 삭제. TTL 자연 만료 = 초대 만료.

### Kafka 토픽

| 토픽 | 발행 시점 | 컨슈머 그룹 |
|---|---|---|
| `workspace.invitation` | 초대 요청 생성 | `travel-core-notification-invitation` |

**메시지 구조**
```json
{
  "invitationId": 42,
  "workspaceId": 7,
  "workspaceTitle": "도쿄 여행",
  "inviterNickname": "박상민",
  "inviteeId": 123
}
```

`NotificationConsumer`에서 수신 후 현재는 log stub.  
FCM/웹소켓 알림 인프라 구축 시 실제 발송 로직으로 교체 예정.

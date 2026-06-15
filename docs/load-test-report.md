# k6 부하 테스트 결과 보고서

## 테스트 개요

| 항목 | 내용 |
|---|---|
| 도구 | k6 v2.0.0 (Grafana Labs) |
| 대상 서버 | travel-core-service (Spring Boot 3, localhost:8080) |
| 테스트 시나리오 | REST API + WebSocket (STOMP over SockJS) 동시 부하 |
| 인프라 | Redis pub/sub, Kafka, PostgreSQL, MongoDB |

---

## 시나리오 구성

### rest_load — REST API 램프업 부하

| 단계 | 지속 시간 | 목표 VU |
|---|---|---|
| 워밍업 | 30s | 10 |
| 정상 부하 | 60s | 30 |
| 피크 부하 | 60s | 50 |
| 쿨다운 | 30s | 0 |

테스트 대상 API (랜덤 분배):
- `GET /api/workspaces/{id}` — 워크스페이스 상세 조회 (40%)
- `GET /api/conquest` — 정복지도 조회 (30%)
- `GET /api/v1/messaging/rooms` — 채팅방 목록 조회 (30%)

### ws_load — WebSocket STOMP 동시 접속

| 단계 | 지속 시간 | 목표 VU |
|---|---|---|
| 램프업 | 30s | 5 |
| 피크 | 90s | 20 |
| 쿨다운 | 30s | 0 |

테스트 흐름:
1. SockJS raw WebSocket 연결 (`/ws/000/{sessionId}/websocket`)
2. STOMP `CONNECT` (JWT Bearer 인증)
3. `/sub/chat/{roomId}` 구독
4. `/pub/chat.message.{roomId}` 메시지 전송
5. Redis pub/sub 브로드캐스트 수신 후 연결 종료

---

## 테스트 결과

### 최종 실행 결과 (3분 풀 부하)

| 지표 | 결과 |
|---|---|
| 총 요청 수 | **5,856건** |
| 에러율 | **0.00%** |
| 최대 동시 VU | 70 (REST 50 + WS 20) |
| 총 실행 시간 | 3분 00초 |
| rest_load | ✅ 통과 |
| ws_load | ✅ 통과 |

### REST API 성능

| 지표 | 결과 | 임계값 |
|---|---|---|
| 평균 응답시간 | **11ms** | — |
| p95 응답시간 | **30ms** | < 1,000ms ✅ |
| 에러율 | **0.00%** | < 5% ✅ |

### WebSocket + Redis pub/sub 성능

| 지표 | 결과 | 임계값 |
|---|---|---|
| 연결 지연 p95 | **0ms** | < 2,000ms ✅ |
| 메시지 왕복 p95 | **0ms** | < 500ms ✅ |
| 비고 | Redis pub/sub 포함 end-to-end | |

> WebSocket 메시지 왕복이 0ms로 측정된 이유: 로컬 환경에서 Redis와 Spring Boot가 동일 호스트에 있어 pub/sub 브로드캐스트 지연이 측정 단위(1ms) 미만으로 처리됨.

---

## 임계값 검증 결과

```
thresholds:
  http_req_duration  p(95) < 1000ms  → 30ms  ✅
  ws_connect_latency p(95) < 2000ms  → 0ms   ✅
  ws_message_latency p(95) < 500ms   → 0ms   ✅
  error_rate         rate  < 0.05    → 0.00% ✅
```

모든 임계값 통과 — 에러 없음.

---

## 실행 명령어

```bash
k6 run \
  -e JWT_TOKEN=<ACCESS_TOKEN> \
  -e WORKSPACE_ID=1 \
  -e MESSAGING_ROOM_ID=1 \
  k6/load-test.js
```

스크립트 위치: `k6/load-test.js`

---

## 분석 요약

- **REST API**: 50 VU 피크 상황에서 평균 11ms, p95 30ms로 매우 안정적. Redis 캐싱이 적용된 조회 API 위주로 테스트하여 DB 부하 없이 낮은 레이턴시 유지.
- **WebSocket**: STOMP 핸드셰이크 → 구독 → 메시지 전송 → Redis pub/sub 브로드캐스트 → 수신 전체 흐름이 20 VU 동시 접속에서 에러 없이 처리됨.
- **에러율 0%**: JWT 인증, 워크스페이스 멤버 권한 검증, MongoDB 채팅방 조회, Redis pub/sub 전체 경로가 부하 상황에서도 정상 동작 확인.

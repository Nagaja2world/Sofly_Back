/**
 * Sofly 백엔드 k6 부하 테스트 — WebSocket + Redis pub/sub
 *
 * 실행:
 *   k6 run \
 *     -e JWT_TOKEN=<토큰> \
 *     -e WORKSPACE_ID=<워크스페이스ID> \
 *     -e MESSAGING_ROOM_ID=<메시징룸ID> \
 *     k6/load-test.js
 *
 * 설치: brew install k6
 */

import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// ── 설정 ──────────────────────────────────────────────────────
const BASE_URL        = 'http://localhost:8080';
const WS_URL          = 'ws://localhost:8080';
const JWT_TOKEN       = __ENV.JWT_TOKEN       || 'YOUR_JWT_TOKEN_HERE';
const WORKSPACE_ID    = __ENV.WORKSPACE_ID    || '1';
const MESSAGING_ROOM_ID = __ENV.MESSAGING_ROOM_ID || '1';

// ── 커스텀 메트릭 ────────────────────────────────────────────
const wsConnectLatency  = new Trend('ws_connect_latency', true);
const wsMsgLatency      = new Trend('ws_message_latency', true);
const restLatency       = new Trend('rest_latency',       true);
const errorRate         = new Rate('error_rate');
const totalRequests     = new Counter('total_requests');

// ── 부하 시나리오 ────────────────────────────────────────────
export const options = {
  scenarios: {
    // REST API 부하 테스트 (램프업 → 피크 → 유지 → 쿨다운)
    rest_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },  // 워밍업
        { duration: '60s', target: 30 },  // 정상 부하
        { duration: '60s', target: 50 },  // 피크 부하
        { duration: '30s', target: 0  },  // 쿨다운
      ],
    },
    // WebSocket 동시 접속 테스트
    ws_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 5  },
        { duration: '90s', target: 20 },
        { duration: '30s', target: 0  },
      ],
      startTime: '0s',
    },
  },
  thresholds: {
    'http_req_duration':  ['p(95)<1000'],
    'ws_connect_latency': ['p(95)<2000'],
    'ws_message_latency': ['p(95)<500'],
    'error_rate':         ['rate<0.05'],
  },
};

const httpHeaders = {
  'Content-Type':  'application/json',
  'Authorization': `Bearer ${JWT_TOKEN}`,
};

// ── 메인: 시나리오별 분기 ─────────────────────────────────────
export default function () {
  // rest_load 시나리오는 REST, ws_load는 WebSocket
  if (__ENV.K6_SCENARIO === 'ws_load') {
    testWebSocket();
  } else {
    const r = Math.random();
    if (r < 0.4)      testWorkspace();
    else if (r < 0.7) testConquestMap();
    else              testMessagingRooms();
  }
  sleep(1);
}

// ── REST 1: 워크스페이스 조회 ─────────────────────────────────
function testWorkspace() {
  const res = http.get(`${BASE_URL}/api/workspaces/${WORKSPACE_ID}`, { headers: httpHeaders });
  const ok = check(res, { 'workspace 200': (r) => r.status === 200 });
  restLatency.add(res.timings.duration);
  errorRate.add(!ok);
  totalRequests.add(1);
}

// ── REST 2: 정복지도 조회 ─────────────────────────────────────
function testConquestMap() {
  const res = http.get(`${BASE_URL}/api/conquest`, { headers: httpHeaders });
  const ok = check(res, { 'conquest 200': (r) => r.status === 200 });
  restLatency.add(res.timings.duration);
  errorRate.add(!ok);
  totalRequests.add(1);
}

// ── REST 3: 채팅방 목록 조회 ─────────────────────────────────
function testMessagingRooms() {
  const res = http.get(`${BASE_URL}/api/v1/messaging/rooms`, { headers: httpHeaders });
  const ok = check(res, { 'messaging rooms 200': (r) => r.status === 200 });
  restLatency.add(res.timings.duration);
  errorRate.add(!ok);
  totalRequests.add(1);
}

// ── WebSocket STOMP: 연결 → 구독 → 메시지 전송 ────────────────
function testWebSocket() {
  // SockJS raw WebSocket 경로: /ws/{server}/{sessionId}/websocket
  const sessionId = Math.random().toString(36).substring(2, 10);
  const url = `${WS_URL}/ws/000/${sessionId}/websocket`;

  const connectStart = Date.now();
  let connected = false;
  let subscribed = false;
  let msgSentAt  = 0;

  const res = ws.connect(url, {}, function (socket) {
    socket.on('open', () => {
      wsConnectLatency.add(Date.now() - connectStart);
    });

    socket.on('message', (raw) => {
      if (raw === 'o') {
        // SockJS open → STOMP CONNECT
        socket.send(stompFrame('CONNECT', {
          'accept-version': '1.1,1.0',
          'host': 'localhost',
          'Authorization': `Bearer ${JWT_TOKEN}`,
        }));
        return;
      }

      const body = parseSockJS(raw);
      if (!body) return;

      if (body.startsWith('CONNECTED') && !connected) {
        connected = true;
        // 구독 + 즉시 메시지 전송 (RECEIPT 기다리지 않음)
        socket.send(stompFrame('SUBSCRIBE', {
          'id': 'sub-0',
          'destination': `/sub/chat/${MESSAGING_ROOM_ID}`,
        }));
        // 구독 직후 메시지 전송 → Redis pub/sub 왕복 시간 측정
        msgSentAt = Date.now();
        socket.send(stompFrame('SEND', {
          'destination': `/pub/chat.message.${MESSAGING_ROOM_ID}`,
          'content-type': 'application/json',
        }, JSON.stringify({
          content: `k6 부하테스트 ${sessionId}`,
          type: 'TEXT',
        })));
        return;
      }

      // Redis pub/sub 거쳐 /sub/chat/{roomId}로 브로드캐스트된 메시지 수신
      if (body.startsWith('MESSAGE') && msgSentAt > 0) {
        wsMsgLatency.add(Date.now() - msgSentAt);
        msgSentAt = 0;
        socket.close();
      }
    });

    socket.on('error', (e) => {
      errorRate.add(1);
    });

    // 5초 타임아웃
    socket.setTimeout(() => {
      socket.close();
    }, 5000);
  });

  const ok = check(res, { 'ws status 101': (r) => r && r.status === 101 });
  errorRate.add(!ok);
  totalRequests.add(1);
}

// ── STOMP 프레임 생성 (SockJS 래핑 포함) ──────────────────────
function stompFrame(command, headers = {}, body = '') {
  let frame = command + '\n';
  for (const [k, v] of Object.entries(headers)) {
    frame += `${k}:${v}\n`;
  }
  frame += '\n' + body + '\x00';
  // SockJS data frame 형식: a["..."]
  return `a[${JSON.stringify(frame)}]`;
}

// ── SockJS 프레임 파싱 ────────────────────────────────────────
function parseSockJS(raw) {
  if (!raw || raw === 'h') return null;   // heartbeat
  if (raw === 'o') return null;           // open (별도 처리)
  if (raw.startsWith('a')) {
    try {
      const arr = JSON.parse(raw.slice(1));
      return arr[0] || null;
    } catch { return null; }
  }
  return null;
}

// ── 결과 요약 ─────────────────────────────────────────────────
export function handleSummary(data) {
  const m = data.metrics;
  const p95 = (metric) => metric ? metric.values['p(95)'].toFixed(0) + 'ms' : 'N/A';
  const avg = (metric) => metric ? metric.values['avg'].toFixed(0) + 'ms' : 'N/A';

  console.log(`
========================================
  Sofly WebSocket + REST 부하 테스트 결과
========================================
  총 요청 수         : ${m.total_requests?.values?.count ?? 'N/A'}
  에러율             : ${((m.error_rate?.values?.rate ?? 0) * 100).toFixed(2)}%

  [REST API]
    평균 응답시간      : ${avg(m.rest_latency)}
    p95 응답시간      : ${p95(m.rest_latency)}

  [WebSocket]
    연결 지연 (p95)   : ${p95(m.ws_connect_latency)}
    메시지 왕복 (p95) : ${p95(m.ws_message_latency)}
    (Redis pub/sub 포함 end-to-end)

  전체 p95           : ${p95(m.http_req_duration)}
========================================
`);

  return { 'k6/result-summary.json': JSON.stringify(data, null, 2) };
}

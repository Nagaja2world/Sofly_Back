/**
 * Sofly 백엔드 k6 부하 테스트
 *
 * 실행 전 준비:
 *   1. 서버 기동:  ./gradlew :services:travel-core-service:bootRun
 *   2. JWT 토큰:  아래 JWT_TOKEN을 Swagger(http://localhost:8080/core/swagger-ui)에서
 *                로그인 후 발급받은 값으로 교체
 *   3. ID 값:    WORKSPACE_ID, CHAT_ROOM_ID를 실제 값으로 교체
 *
 * 실행:
 *   k6 run k6/load-test.js
 *
 * 설치:
 *   brew install k6
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// ── 테스트 대상 설정 ──────────────────────────────────────────
const BASE_URL    = 'http://localhost:8080';
const JWT_TOKEN   = __ENV.JWT_TOKEN || 'YOUR_JWT_TOKEN_HERE';
const WORKSPACE_ID = __ENV.WORKSPACE_ID || '1';
const CHAT_ROOM_ID = __ENV.CHAT_ROOM_ID || '1';

// ── 커스텀 메트릭 ────────────────────────────────────────────
const chatLatency      = new Trend('chat_latency',      true);
const conquestLatency  = new Trend('conquest_latency',  true);
const workspaceLatency = new Trend('workspace_latency', true);
const errorRate        = new Rate('error_rate');
const totalRequests    = new Counter('total_requests');

// ── 부하 시나리오 ────────────────────────────────────────────
export const options = {
  scenarios: {
    // 시나리오 1: 점진적 부하 증가 (Ramp-up)
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 5  },  // 0 → 5 VU
        { duration: '60s', target: 10 },  // 5 → 10 VU (정상 부하)
        { duration: '30s', target: 20 },  // 10 → 20 VU (피크)
        { duration: '30s', target: 0  },  // 20 → 0 VU (쿨다운)
      ],
    },
  },
  thresholds: {
    // SLO: p95 응답시간 2초 이하, 에러율 1% 미만
    'http_req_duration': ['p(95)<2000'],
    'error_rate':        ['rate<0.01'],
    'chat_latency':      ['p(95)<5000'],  // AI 응답은 여유 있게
  },
};

// ── 공통 헤더 ─────────────────────────────────────────────────
const headers = {
  'Content-Type':  'application/json',
  'Authorization': `Bearer ${JWT_TOKEN}`,
};

// ── 메인 테스트 함수 ──────────────────────────────────────────
export default function () {
  const scenario = Math.random();

  if (scenario < 0.4) {
    testWorkspaceRead();
  } else if (scenario < 0.7) {
    testConquestMap();
  } else {
    testChatMessage();
  }

  sleep(1);
}

// ── 1. 워크스페이스 조회 ─────────────────────────────────────
function testWorkspaceRead() {
  const res = http.get(`${BASE_URL}/api/workspaces/${WORKSPACE_ID}`, { headers });
  const ok = check(res, {
    'workspace 200': (r) => r.status === 200,
  });

  workspaceLatency.add(res.timings.duration);
  errorRate.add(!ok);
  totalRequests.add(1);
}

// ── 2. 정복지도 조회 ─────────────────────────────────────────
function testConquestMap() {
  const res = http.get(`${BASE_URL}/api/conquest/map`, { headers });
  const ok = check(res, {
    'conquest 200': (r) => r.status === 200,
  });

  conquestLatency.add(res.timings.duration);
  errorRate.add(!ok);
  totalRequests.add(1);
}

// ── 3. AI 채팅 메시지 전송 ───────────────────────────────────
function testChatMessage() {
  const prompts = [
    '도쿄 3박 4일 여행 계획 짜줘',
    '오사카 맛집 위주로 2박 3일 코스 알려줘',
    '제주도 렌트카 없이 4박 5일 가능해?',
    '방콕 가족여행 일주일 추천해줘',
  ];
  const message = prompts[Math.floor(Math.random() * prompts.length)];

  const res = http.post(
    `${BASE_URL}/api/chat/rooms/${CHAT_ROOM_ID}/messages`,
    JSON.stringify({ message }),
    { headers, timeout: '30s' }
  );

  const ok = check(res, {
    'chat 200': (r) => r.status === 200,
    'chat has response': (r) => {
      try { return JSON.parse(r.body).response !== undefined; }
      catch { return false; }
    },
  });

  chatLatency.add(res.timings.duration);
  errorRate.add(!ok);
  totalRequests.add(1);
}

// ── 테스트 종료 후 요약 출력 ──────────────────────────────────
export function handleSummary(data) {
  return {
    'k6/result-summary.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data),
  };
}

function textSummary(data) {
  const m = data.metrics;
  const p95 = (metric) => metric ? metric.values['p(95)'].toFixed(0) : 'N/A';
  const avg = (metric) => metric ? metric.values['avg'].toFixed(0) : 'N/A';

  return `
========================================
  Sofly 부하 테스트 결과
========================================
  총 요청 수       : ${m.total_requests?.values?.count ?? 'N/A'}
  에러율           : ${((m.error_rate?.values?.rate ?? 0) * 100).toFixed(2)}%

  [워크스페이스 조회]  avg=${avg(m.workspace_latency)}ms  p95=${p95(m.workspace_latency)}ms
  [정복지도 조회]      avg=${avg(m.conquest_latency)}ms   p95=${p95(m.conquest_latency)}ms
  [AI 채팅]            avg=${avg(m.chat_latency)}ms       p95=${p95(m.chat_latency)}ms

  전체 p95 응답시간 : ${p95(m.http_req_duration)}ms
  전체 평균 응답시간 : ${avg(m.http_req_duration)}ms
========================================
`;
}

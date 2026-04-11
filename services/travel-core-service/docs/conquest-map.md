# Conquest Map — 기술 문서

> **대상 독자** 프론트엔드 개발자 (React 구현 시 참고)
> **테스트 페이지** `http://localhost:8080/conquest-test.html`
> **Base URL** `http://localhost:8080`
> **인증 방식** JWT Bearer Token

---

## 목차

1. [기능 개요](#1-기능-개요)
2. [테스트 페이지 사용법](#2-테스트-페이지-사용법)
3. [기술 스택 & 라이브러리](#3-기술-스택--라이브러리)
4. [API 명세](#4-api-명세)
5. [데이터 모델](#5-데이터-모델)
6. [지도 구현 상세](#6-지도-구현-상세)
7. [UI 컴포넌트 구조](#7-ui-컴포넌트-구조)
8. [자동 상태 전환 로직](#8-자동-상태-전환-로직)
9. [React 구현 가이드](#9-react-구현-가이드)

---

## 1. 기능 개요

Conquest Map은 사용자의 여행 발자국을 세계 지도 위에 시각화하는 기능입니다.

| 기능 | 설명 |
|------|------|
| **국가 색칠** | 방문 상태에 따라 세계 지도 국가를 3가지 색으로 표시 |
| **도시 핀** | 방문한 도시를 지도 위 핀으로 표시, 줌 레벨에 따라 클러스터링 |
| **항공 경로 Arc** | 워크스페이스에 저장된 항공편을 곡선 경로로 시각화 |
| **상태 수동 변경** | 국가/도시 클릭 → 미방문 / 예정 / 완료 수동 변경 |
| **워크스페이스 연동** | 국가 클릭 시 해당 국가와 연관된 워크스페이스 목록 표시 |
| **여행 통계** | 방문 국가 수, 도시 수, 여행일, 이동거리, 대륙별 분포 |
| **일괄 등록** | 과거 방문 이력을 JSON으로 한번에 등록 |

### 방문 상태 (VisitStatus)

| 상태 | 값 | 색상 | 의미 |
|------|----|------|------|
| 미방문 | `UNVISITED` | `#374151` (어두운 회색) | 아직 방문하지 않은 국가/도시 |
| 방문 예정 | `PLANNED` | `#f59e0b` (amber) | 항공권이 있거나 계획 중인 여행 |
| 방문 완료 | `VISITED` | `#8b5cf6` (purple) | 이미 방문한 국가/도시 |

---

## 2. 테스트 페이지 사용법

### 초기 설정

```
1. http://localhost:8080/conquest-test.html 접속
2. 헤더 우측 "Mapbox Token" 입력 (pk.eyJ1... 형식의 퍼블릭 토큰)
3. "지도 로드" 버튼 클릭 → 세계 지도 렌더링
4. "Base URL" 확인 (기본값: http://localhost:8080)
5. "JWT Token" 입력 (로그인 후 발급된 accessToken)
6. "데이터 로드" 버튼 클릭 → 지도에 방문 데이터 반영
```

> 입력한 토큰과 Base URL은 `localStorage`에 자동 저장됩니다 (새로고침 후에도 유지).

### 주요 인터랙션

| 동작 | 결과 |
|------|------|
| 국가 클릭 | 오른쪽 사이드바에 워크스페이스 목록 슬라이드인 |
| 사이드바 "상태 변경" 클릭 | 해당 국가의 방문 상태 변경 모달 |
| 워크스페이스 카드 클릭 | 해당 여행의 항공 경로만 지도에 하이라이트 |
| "전체 경로" 버튼 클릭 | 모든 항공 경로 표시로 복원 |
| 도시 핀 클릭 | 팝업 표시 (도시명, 상태, 방문 횟수) + 상태 변경 버튼 |
| 클러스터 클릭 | 해당 지역으로 줌인 |
| 통계 패널 "일괄 등록" | JSON 편집기로 과거 방문 이력 등록 |
| 통계 패널 닫기(✕) | 패널이 왼쪽으로 슬라이드아웃 (지도 가리지 않음) |

---

## 3. 기술 스택 & 라이브러리

### 테스트 페이지 (`conquest-test.html`)

| 라이브러리 | 버전 | 역할 | CDN |
|-----------|------|------|-----|
| **Mapbox GL JS** | v3.4.0 | 세계 지도 렌더링, 레이어 관리, 이벤트 처리 | `api.mapbox.com/mapbox-gl-js/v3.4.0/` |
| Vanilla JS | ES2020+ | 상태 관리, API 호출, DOM 조작 | — |

> 클러스터링은 Mapbox GL JS의 **내장 클러스터 기능** (`cluster: true`)을 사용합니다.
> 별도의 Supercluster 라이브러리는 사용하지 않습니다.

### React 구현 시 권장 스택

| 라이브러리 | 역할 |
|-----------|------|
| `react-map-gl` 또는 `mapbox-gl` 직접 사용 | 지도 렌더링 |
| `deck.gl` (`ArcLayer`) | 항공 경로 Arc 시각화 (고품질) |
| `supercluster` + `use-supercluster` | 도시 핀 클러스터링 |
| `@turf/turf` | 거리 계산, 좌표 보정 등 GeoJSON 유틸리티 |

---

## 4. API 명세

모든 엔드포인트는 `/api/conquest` 아래에 위치하며 JWT 인증이 필요합니다.

### 공통 응답 형식

```json
{
  "success": true,
  "message": null,
  "data": { ... }
}
```

---

### 4-1. 정복 지도 조회

```
GET /api/conquest
Authorization: Bearer {accessToken}
```

방문한 모든 국가와 도시 목록을 반환합니다.

**Response Body**

```json
{
  "success": true,
  "data": {
    "countries": [
      {
        "id": 1,
        "countryCode": "KR",
        "countryName": "대한민국",
        "status": "VISITED",
        "continent": "ASIA",
        "continentName": "아시아",
        "visitCount": 10
      }
    ],
    "cities": [
      {
        "id": 1,
        "cityName": "Seoul",
        "countryCode": "KR",
        "latitude": 37.5665,
        "longitude": 126.978,
        "status": "VISITED",
        "visitCount": 5
      }
    ]
  }
}
```

---

### 4-2. 여행 통계 조회

```
GET /api/conquest/stats
Authorization: Bearer {accessToken}
```

**Response Body**

```json
{
  "success": true,
  "data": {
    "visitedCountryCount": 12,
    "totalCountryCount": 195,
    "visitedCountryPercentage": 6.2,
    "visitedCityCount": 25,
    "totalTravelDays": 84,
    "totalDistanceKm": 48320.5,
    "continentStats": [
      {
        "continent": "ASIA",
        "continentName": "아시아",
        "visitedCountryCount": 5
      },
      {
        "continent": "EUROPE",
        "continentName": "유럽",
        "visitedCountryCount": 4
      }
    ]
  }
}
```

---

### 4-3. 전체 항공 경로 조회

```
GET /api/conquest/routes
Authorization: Bearer {accessToken}
```

저장된 모든 항공편의 출발지/도착지 좌표와 경로 정보를 반환합니다.

**Response Body**

```json
{
  "success": true,
  "data": [
    {
      "flightId": 1,
      "workspaceId": 10,
      "workspaceTitle": "도쿄 여행",
      "departureAirport": "ICN",
      "departureCity": "Seoul",
      "departureCountryCode": "KR",
      "departureLat": 37.4602,
      "departureLng": 126.4407,
      "arrivalAirport": "NRT",
      "arrivalCity": "Tokyo",
      "arrivalCountryCode": "JP",
      "arrivalLat": 35.7647,
      "arrivalLng": 140.3864,
      "departureTime": "2025-03-15T09:30:00",
      "arrivalTime": "2025-03-15T11:45:00",
      "airline": "대한항공",
      "flightNumber": "KE701",
      "distanceKm": 1214.3,
      "routeType": "INTERNATIONAL"
    }
  ]
}
```

---

### 4-4. 워크스페이스별 항공 경로 조회

```
GET /api/conquest/routes/workspaces/{workspaceId}
Authorization: Bearer {accessToken}
```

특정 워크스페이스에 속한 항공편 경로만 반환합니다. 응답 구조는 4-3과 동일.

---

### 4-5. 국가별 워크스페이스 목록 조회

```
GET /api/conquest/countries/{countryCode}/workspaces
Authorization: Bearer {accessToken}
```

국가를 클릭했을 때 해당 국가와 연관된 워크스페이스 목록을 반환합니다.

**Path Variable**

| 파라미터 | 타입 | 예시 |
|---------|------|------|
| `countryCode` | String (ISO alpha-2) | `JP`, `FR`, `US` |

**Response Body**

```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "title": "도쿄 여행 2025",
      "destination": "도쿄",
      "countryCode": "JP",
      "startDate": "2025-03-15",
      "endDate": "2025-03-20",
      "coverImageUrl": "https://...",
      "memberCount": 2
    }
  ]
}
```

---

### 4-6. 국가 방문 상태 변경

```
PUT /api/conquest/countries/{countryCode}/status
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**

```json
{
  "status": "VISITED"
}
```

`status` 허용값: `UNVISITED` | `PLANNED` | `VISITED`

**Response Body** — 변경된 국가 정보 (4-1의 country 객체와 동일 구조)

---

### 4-7. 도시 추가 / 업데이트

```
POST /api/conquest/cities
Authorization: Bearer {accessToken}
Content-Type: application/json
```

도시가 이미 존재하면 업데이트, 없으면 생성합니다. (도시명 + 국가코드 기준)

**Request Body**

```json
{
  "cityName": "Tokyo",
  "countryCode": "JP",
  "latitude": 35.6762,
  "longitude": 139.6503,
  "status": "VISITED"
}
```

**Response** `201 Created` + 생성/업데이트된 도시 정보

---

### 4-8. 도시 방문 상태 변경

```
PUT /api/conquest/cities/{cityId}/status
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**

```json
{
  "status": "VISITED"
}
```

**Response Body** — 변경된 도시 정보 (4-1의 city 객체와 동일 구조)

---

### 4-9. 과거 방문 일괄 등록

```
POST /api/conquest/bulk-import
Authorization: Bearer {accessToken}
Content-Type: application/json
```

국가와 도시를 한번에 등록합니다. `countries`와 `cities` 둘 다 선택적입니다.

**Request Body**

```json
{
  "countries": [
    { "countryCode": "JP", "status": "VISITED" },
    { "countryCode": "FR", "status": "VISITED" }
  ],
  "cities": [
    {
      "cityName": "Tokyo",
      "countryCode": "JP",
      "latitude": 35.6762,
      "longitude": 139.6503,
      "status": "VISITED"
    }
  ]
}
```

**Response** `204 No Content` (응답 바디 없음)

---

## 5. 데이터 모델

### VisitStatus Enum

```
UNVISITED  → 미방문 (기본값)
PLANNED    → 방문 예정 (항공편 등록 시 자동 전환)
VISITED    → 방문 완료 (항공편 출발일 경과 시 자동 전환)
```

### Continent Enum

| 값 | 한국어 표시명 |
|----|-------------|
| `ASIA` | 아시아 |
| `EUROPE` | 유럽 |
| `NORTH_AMERICA` | 북아메리카 |
| `SOUTH_AMERICA` | 남아메리카 |
| `AFRICA` | 아프리카 |
| `OCEANIA` | 오세아니아 |

### RouteType Enum

```
DOMESTIC       → 국내선 (출발/도착 국가코드 동일)
INTERNATIONAL  → 국제선
```

---

## 6. 지도 구현 상세

### 6-1. Mapbox 소스 구성

```
country-boundaries  (vector)
  └─ mapbox://mapbox.country-boundaries-v1
      └─ source-layer: "country_boundaries"
          ├─ property: iso_3166_1      (국가 코드, ISO alpha-2)
          └─ property: worldview       (지도 세계관 필터용)

cities  (geojson, cluster: true)
  └─ 방문 도시 포인트 데이터
      ├─ cluster: true
      ├─ clusterMaxZoom: 8
      └─ clusterRadius: 38

routes  (geojson)
  └─ 항공 경로 곡선 LineString 데이터
```

### 6-2. Mapbox 레이어 구성

| 레이어 ID | 타입 | 소스 | 용도 |
|-----------|------|------|------|
| `c-fill` | fill | country-boundaries | 국가 방문 상태별 색칠 |
| `c-line` | line | country-boundaries | 국가 경계선 (연한 회색) |
| `c-hover` | fill | country-boundaries | 마우스 호버 시 밝기 증가 |
| `c-sel` | line | country-boundaries | 선택된 국가 보라색 테두리 |
| `rt-line` | line | routes | 항공 경로 본선 |
| `rt-glow` | line | routes | 항공 경로 glow 효과 (불투명도 낮은 두꺼운 선) |
| `cl-circle` | circle | cities | 클러스터 원형 |
| `cl-count` | symbol | cities | 클러스터 개수 텍스트 |
| `city-pt` | circle | cities | 개별 도시 핀 |

### 6-3. 국가 색상 표현식

Mapbox `match` 표현식을 동적으로 생성해 국가별 색상을 지정합니다.

```javascript
// API 응답으로 받은 countries 배열 기반으로 생성
const expr = ['match', ['get', 'iso_3166_1']];

for (const country of countries) {
  if (country.status === 'VISITED') {
    expr.push(country.countryCode, '#8b5cf6'); // purple
  } else if (country.status === 'PLANNED') {
    expr.push(country.countryCode, '#f59e0b'); // amber
  }
  // UNVISITED는 기본값으로 처리
}

expr.push('#1e293b'); // 기본값 (미방문)

map.setPaintProperty('c-fill', 'fill-color', expr);
```

### 6-4. 항공 경로 Arc 생성

직선 경로 대신 Quadratic Bezier 곡선으로 호 모양을 만듭니다.

```javascript
function arc(from, to, numPoints = 80) {
  let [x1, y1] = from;
  let [x2, y2] = to;

  // 날짜변경선(antimeridian) 처리
  if (Math.abs(x2 - x1) > 180) {
    x2 += x2 > x1 ? -360 : 360;
  }

  // 이차 베지어 제어점: 두 지점 중간에서 위도 방향으로 올림
  const mx = (x1 + x2) / 2;
  const my = (y1 + y2) / 2;
  const dist = Math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2);
  const ctrlY = my + dist * 0.28; // 거리에 비례해 호의 높이 결정

  const points = [];
  for (let i = 0; i <= numPoints; i++) {
    const t = i / numPoints, mt = 1 - t;
    points.push([
      mt * mt * x1 + 2 * mt * t * mx + t * t * x2,
      mt * mt * y1 + 2 * mt * t * ctrlY + t * t * y2
    ]);
  }
  return points; // GeoJSON LineString의 coordinates로 사용
}
```

> **주의**: 경로 데이터의 `departureLng`, `departureLat`, `arrivalLng`, `arrivalLat` 값이 `0`이거나 누락된 경우 해당 경로는 렌더링에서 제외됩니다.

### 6-5. 도시 클러스터링

Mapbox의 내장 GeoJSON 클러스터링을 사용합니다.

```javascript
map.addSource('cities', {
  type: 'geojson',
  data: citiesGeoJSON,
  cluster: true,
  clusterMaxZoom: 8,   // 줌 8 이상에서 클러스터 해제
  clusterRadius: 38    // 38px 반경 내 핀을 하나로 묶음
});
```

클러스터 클릭 시 `getClusterExpansionZoom`으로 자동 줌인:

```javascript
map.on('click', 'cl-circle', e => {
  const clusterId = e.features[0].properties.cluster_id;
  map.getSource('cities').getClusterExpansionZoom(clusterId, (err, zoom) => {
    if (!err) map.easeTo({ center: e.features[0].geometry.coordinates, zoom });
  });
});
```

### 6-6. 세계관(Worldview) 필터

국가 경계선이 중복 렌더링되지 않도록 worldview 필터를 적용합니다.

```javascript
const WV_FILTER = [
  'any',
  ['==', ['get', 'worldview'], 'all'],  // 모든 세계관에서 공통인 국경
  ['==', ['get', 'worldview'], 'US']    // 미국 세계관 기준 추가 보완
];
```

---

## 7. UI 컴포넌트 구조

```
Header
├─ Mapbox Token 입력 + 지도 로드 버튼
├─ Base URL 입력
└─ JWT Token 입력 + 데이터 로드 버튼

MapWrapper (position: relative)
├─ #map (Mapbox GL 렌더링 대상)
├─ #initOverlay   (지도 로드 전 안내 오버레이)
├─ #statsPanel    (좌측 통계 패널, 고정 오버레이)
├─ #legend        (좌측 하단, 색상 범례)
├─ #routeBar      (하단 중앙, 경로 필터 버튼)
├─ #wsSidebar     (우측 슬라이드 사이드바, 워크스페이스 목록)
└─ #statusModal   (상태 변경 모달, 국가/도시 공통)
```

### 레이아웃 사이즈 참고

| 요소 | 크기 |
|------|------|
| 헤더 | 고정 높이, `flex-wrap: wrap` |
| 통계 패널 | 너비 252px, 좌측 14px 오프셋 |
| 워크스페이스 사이드바 | 너비 310px, 우측 슬라이드인 (`translateX` 트랜지션) |
| 경로 컨트롤 바 | 하단 중앙 고정 (`left: 50%; transform: translateX(-50%)`) |

---

## 8. 자동 상태 전환 로직

프론트엔드에서 직접 구현하지 않아도 백엔드가 자동으로 처리하는 로직입니다.

### 항공편 등록 → PLANNED 자동 전환

```
SavedFlight 저장 (FlightSavedEvent 발행)
  └─ ConquestMapService.onFlightSaved() 호출
      ├─ 해당 워크스페이스의 모든 멤버에 대해
      ├─ 도착 국가 → PLANNED 상태로 전환
      └─ 도착 도시 → PLANNED 상태로 전환 (AirportInfoService로 공항→도시 매핑)
```

### 스케줄러 → VISITED 자동 전환

```
Spring Scheduler (매일 실행)
  └─ ConquestMapService.promotePlannedToVisited()
      ├─ PLANNED 상태인 모든 국가/도시 조회
      └─ 해당 항공편 출발 시각이 현재보다 이전이면 VISITED로 전환
```

> **프론트엔드 영향**: 데이터 로드(`GET /api/conquest`) 시 항상 최신 상태가 반영됩니다. 폴링이나 웹소켓은 필요 없습니다.

---

## 9. React 구현 가이드

### 9-1. 권장 상태 구조

```typescript
interface ConquestState {
  // 서버 데이터
  countries: VisitedCountryResponse[];
  cities: VisitedCityResponse[];
  stats: ConquestStatsResponse | null;
  routes: TripRouteResponse[];

  // UI 상태
  selectedCountryCode: string | null;  // 클릭된 국가
  routeFilter: 'all' | 'none' | number; // number = workspaceId
  isWsSidebarOpen: boolean;
  workspaces: WorkspaceConquestResponse[]; // 선택 국가의 워크스페이스
}
```

### 9-2. API 호출 흐름

```
컴포넌트 마운트
  ├─ GET /api/conquest    → countries, cities 상태 업데이트 → 지도 레이어 업데이트
  ├─ GET /api/conquest/stats  → 통계 패널 업데이트
  └─ GET /api/conquest/routes → 경로 레이어 업데이트

국가 클릭
  └─ GET /api/conquest/countries/{code}/workspaces → 사이드바 표시

상태 변경 확인
  ├─ PUT /api/conquest/countries/{code}/status
  └─ (완료 후) GET /api/conquest 재호출 → 지도 갱신

워크스페이스 카드 클릭
  └─ GET /api/conquest/routes/workspaces/{id} → 경로 레이어 갱신
```

### 9-3. Mapbox 레이어 색상 동적 업데이트

```typescript
// countries 배열 변경 시 호출
function updateCountryColors(map: mapboxgl.Map, countries: VisitedCountryResponse[]) {
  const colorExpr: mapboxgl.Expression = ['match', ['get', 'iso_3166_1']];

  for (const c of countries) {
    if (c.status === 'VISITED') colorExpr.push(c.countryCode, '#8b5cf6');
    else if (c.status === 'PLANNED') colorExpr.push(c.countryCode, '#f59e0b');
  }
  colorExpr.push('#1e293b'); // fallback

  map.setPaintProperty('country-fill', 'fill-color', colorExpr);
}
```

### 9-4. 도시 GeoJSON 변환

```typescript
function citiesToGeoJSON(cities: VisitedCityResponse[]): GeoJSON.FeatureCollection {
  return {
    type: 'FeatureCollection',
    features: cities.map(city => ({
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [city.longitude, city.latitude] },
      properties: {
        id: city.id,
        cityName: city.cityName,
        countryCode: city.countryCode,
        status: city.status,
        visitCount: city.visitCount,
        color: city.status === 'VISITED' ? '#8b5cf6'
             : city.status === 'PLANNED' ? '#f59e0b' : '#374151',
      }
    }))
  };
}
```

### 9-5. deck.gl ArcLayer 사용 시 (권장)

테스트 페이지는 Mapbox LineString으로 구현했지만, React에서는 deck.gl `ArcLayer`가 더 고품질입니다.

```typescript
import { ArcLayer } from '@deck.gl/layers';

const arcLayer = new ArcLayer({
  id: 'flight-arcs',
  data: routes,
  getSourcePosition: (r) => [r.departureLng, r.departureLat],
  getTargetPosition: (r) => [r.arrivalLng, r.arrivalLat],
  getSourceColor: [167, 139, 250, 180],   // #a78bfa
  getTargetColor: [139, 92, 246, 180],    // #8b5cf6
  getWidth: 2,
  greatCircle: true, // 대원 경로 자동 계산 (antimeridian 처리 포함)
});
```

> `greatCircle: true` 옵션 사용 시 antimeridian 처리가 자동으로 됩니다.

### 9-6. 상태별 색상 상수

```typescript
export const STATUS_COLORS = {
  VISITED:   { fill: '#8b5cf6', stroke: 'rgba(167,139,250,0.5)', label: '방문 완료' },
  PLANNED:   { fill: '#f59e0b', stroke: 'rgba(251,191,36,0.5)',  label: '방문 예정' },
  UNVISITED: { fill: '#374151', stroke: 'rgba(107,114,128,0.3)', label: '미방문'    },
} as const;
```

### 9-7. 주요 엣지 케이스

| 케이스 | 처리 방법 |
|--------|----------|
| 국가 데이터가 없는 경우 (API 미등록 국가) | `PUT /countries/{code}/status` 호출하면 백엔드가 자동 생성 |
| 도시 좌표가 null인 경우 | 해당 도시는 GeoJSON에 포함하지 않음 |
| 경로 출발지/도착지 좌표가 0인 경우 | 해당 경로는 필터링해 렌더링 제외 |
| 날짜변경선을 넘는 경로 | deck.gl `greatCircle: true` 또는 수동 경도 보정 (`±360`) |
| 동일 위치 다수 도시 | 클러스터링으로 자동 처리됨 |

---

## 참고 링크

- [Mapbox GL JS 공식 문서](https://docs.mapbox.com/mapbox-gl-js/api/)
- [Mapbox Country Boundaries 타일셋](https://docs.mapbox.com/data/tilesets/reference/mapbox-country-boundaries-v1/)
- [deck.gl ArcLayer](https://deck.gl/docs/api-reference/layers/arc-layer)
- [react-map-gl](https://visgl.github.io/react-map-gl/)

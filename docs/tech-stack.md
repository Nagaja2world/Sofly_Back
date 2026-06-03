# Sofly 백엔드 기술 스택

---

## 백엔드 (Java)

### 언어 & 프레임워크

| 기술 | 버전 / 설명 |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.x |
| Spring Web MVC | REST API |
| Spring Security | 인증 / 인가 |
| Spring Data JPA | ORM |
| Spring AOP | 워크스페이스 권한 체크 (`@RequireWorkspaceMember`) |
| Spring WebSocket | 실시간 채팅 |
| Spring Cloud OpenFeign | 서비스 간 HTTP 통신 |
| Lombok | 코드 자동 생성 |

### 인증 & 보안

| 기술 | 설명 |
|---|---|
| JWT | Access / Refresh Token (Refresh는 Redis 저장) |
| OAuth2 | Social Login — Google, Kakao, Naver |
| Spring Security | Stateless 세션 전략 |

### 데이터베이스

| 기술 | 용도 |
|---|---|
| PostgreSQL | 운영 DB |
| MongoDB | 문서형 데이터 저장 |
| Redis | Refresh Token / AI 채팅 메모리 캐시(24h TTL) / 호텔 검색 결과 캐시 |
| H2 | 테스트용 인메모리 DB |

### 클라우드 & 스토리지

| 기술 | 용도 |
|---|---|
| AWS S3 | 앨범 사진 저장 |
| Cloudflare R2 | S3 호환 오브젝트 스토리지 |

### 메시지 큐

| 기술 | 용도 |
|---|---|
| Apache Kafka | 이벤트 기반 처리 (예: 항공편 저장 → 정복지도 자동 업데이트) |

### 외부 서비스

| 서비스 | 용도 |
|---|---|
| Google Gemini 2.5 Flash (Spring AI) | AI 여행 플래너 채팅 |
| Google OAuth2 | 소셜 로그인 |
| Kakao OAuth2 | 소셜 로그인 |
| Naver OAuth2 | 소셜 로그인 |
| Google Places API | 장소 검색 & 사진 |
| Booking.com (RapidAPI) | 항공권 / 호텔 검색 |
| Google Calendar API | 캘린더 연동 |

### 관측성 (Observability)

| 기술 | 용도 |
|---|---|
| Prometheus + Micrometer | 메트릭 수집 |
| Grafana | 메트릭 시각화 대시보드 |
| Zipkin + Brave | 분산 트레이싱 |
| cAdvisor | 컨테이너 리소스 모니터링 |
| Dozzle | 컨테이너 로그 뷰어 |

### 문서화

| 기술 | 설명 |
|---|---|
| SpringDoc OpenAPI | Swagger UI 자동 생성 |

---

## 아키텍처

| 서비스 | 포트 | 아키텍처 패턴 | 역할 |
|---|---|---|---|
| `travel-core-service` | 8080 | 레이어드 아키텍처 (도메인 중심) | 인증, 워크스페이스, AI 채팅, 일정, 앨범, 정복지도 |
| `travel-supply-service` | 8081 | 헥사고날 아키텍처 (Ports & Adapters) | 항공권/호텔 외부 공급사 연동, 장소 검색 |

---

## 인프라

| 기술 | 설명 |
|---|---|
| Docker | 멀티스테이지 빌드 (`eclipse-temurin:21` 기반) |
| Gradle | 모노레포 멀티모듈 빌드 |
| Nginx | Blue-Green 무중단 배포 |
| GitHub Actions | 서비스별 독립 CI/CD 파이프라인 |
| Redis Commander | Redis 로컬 개발 편의 UI |
| Kafka UI | Kafka 로컬 개발 편의 UI |

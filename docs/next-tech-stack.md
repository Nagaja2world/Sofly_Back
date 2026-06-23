# Sofly 다음 프로젝트 기술 스택 로드맵

> 캡스톤 프로젝트(Sofly)에서 사용한 스택을 기반으로, 다음 프로젝트에서 추가하거나 발전시킬 기술 목록.

---

## 현재 스택 요약

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

### 인프라

| 기술 | 설명 |
|---|---|
| Docker | 멀티스테이지 빌드 (`eclipse-temurin:21` 기반) |
| Gradle | 모노레포 멀티모듈 빌드 |
| Nginx | Blue-Green 무중단 배포 |
| GitHub Actions | 서비스별 독립 CI/CD 파이프라인 |

### 아키텍처

| 서비스 | 포트 | 패턴 | 역할 |
|---|---|---|---|
| `travel-core-service` | 8080 | 레이어드 아키텍처 (도메인 중심) | 인증, 워크스페이스, AI 채팅, 일정, 앨범, 정복지도 |
| `travel-supply-service` | 8081 | 헥사고날 아키텍처 (Ports & Adapters) | 항공권/호텔 외부 공급사 연동, 장소 검색 |

---

## 다음 프로젝트 추가/발전 스택

### 단기 — 바로 적용 가능한 것들

#### Testcontainers

| 항목 | 내용 |
|---|---|
| 현재 문제 | H2 인메모리 DB로 테스트 → PostgreSQL/Redis와 방언 차이로 운영 환경에서 예상치 못한 버그 발생 |
| 해결 | 실제 PostgreSQL, Redis, MongoDB 컨테이너를 띄워서 통합 테스트 |
| 장점 | 배포 전에 놓치는 버그 감소, 테스트 신뢰도 향상 |
| 학습 비용 | 낮음 — 기존 JUnit 테스트 구조 그대로 사용 |

#### OpenTelemetry

| 항목 | 내용 |
|---|---|
| 현재 문제 | Prometheus, Zipkin, Micrometer가 각각 별도로 운영되어 파이프라인이 분산됨 |
| 해결 | OpenTelemetry Collector 하나로 메트릭 / 트레이스 / 로그를 단일 파이프라인으로 통합 |
| 장점 | 관측성 스택 단순화, 벤더 종속성 제거 |
| 학습 비용 | 낮음 — Spring Boot 자동 설정 지원 |

---

### 중기 — 아키텍처 레벨 업그레이드

#### Kubernetes + Helm

| 항목 | 내용 |
|---|---|
| 현재 상태 | Docker Compose + Nginx 블루그린 배포 |
| 다음 단계 | 로컬: `kind` 또는 `minikube` / 운영: AWS EKS |
| 장점 | 블루그린/카나리 배포 체계화, 자동 스케일링, 셀프힐링 |
| 취업 관련성 | 실무에서 가장 많이 요구되는 인프라 기술 |

```
Docker Compose  →  Kubernetes + Helm
Nginx 블루그린  →  K8s Deployment + Service + Ingress
```

#### gRPC (서비스 간 통신)

| 항목 | 내용 |
|---|---|
| 현재 상태 | OpenFeign 준비만 된 상태, 서비스 간 통신 미사용 |
| 다음 단계 | 내부 서비스 간 통신에 gRPC 도입 |
| 장점 | REST 대비 빠른 직렬화(protobuf), 명확한 스키마(.proto), 양방향 스트리밍 지원 |
| 라이브러리 | `grpc-spring-boot-starter` — Spring Boot 3.x 호환 |

#### CQRS / 읽기-쓰기 분리

| 항목 | 내용 |
|---|---|
| 대상 도메인 | AI 채팅 이력, 여행 로그, 정복지도 |
| 쓰기 모델 | JPA + PostgreSQL (Command) |
| 읽기 모델 | MongoDB 또는 Elasticsearch (Query) |
| 동기화 방식 | Kafka / SQS 이벤트로 읽기 모델 자동 갱신 |
| 장점 | 읽기 성능 향상, 복잡한 쿼리 분리, 이벤트 소싱 패턴 연습 |

---

### 장기 — AI 기능 심화

#### RAG + Vector DB

| 항목 | 내용 |
|---|---|
| 현재 상태 | Gemini + Redis 단순 메모리 캐시 |
| 다음 단계 | 여행 정보를 벡터 임베딩으로 저장 → 유사도 기반 검색으로 AI 답변 품질 향상 |
| 옵션 1 | `pgvector` — 기존 PostgreSQL에 확장만 추가, 인프라 변경 없음 |
| 옵션 2 | `Pinecone` — 관리형 Vector DB, 별도 인프라 불필요 |
| Spring AI 지원 | Vector Store 추상화 이미 내장, 코드 변경 최소화 |

```
현재: Gemini → Redis 캐시 → 응답
RAG:  사용자 질문 → 임베딩 → Vector DB 유사도 검색 → 관련 여행 정보 주입 → Gemini → 응답
```

#### ArgoCD (GitOps)

| 항목 | 내용 |
|---|---|
| 현재 상태 | GitHub Actions가 빌드 + 배포 모두 담당 |
| 다음 단계 | GitHub Actions는 빌드/이미지 푸시만, ArgoCD가 Git 상태 기준으로 K8s 자동 동기화 |
| 장점 | 배포 상태가 Git에 선언적으로 관리됨, 롤백이 git revert로 가능 |
| 전제 조건 | Kubernetes 도입 후 자연스럽게 세트로 따라오는 패턴 |

#### Terraform / AWS CDK

| 항목 | 내용 |
|---|---|
| 현재 상태 | 인프라 수동 설정 + docker-compose |
| 다음 단계 | IaC(Infrastructure as Code)로 인프라 코드화 |
| 옵션 1 | `Terraform` — 멀티 클라우드, 커뮤니티 넓음 |
| 옵션 2 | `AWS CDK` — Java/TypeScript로 AWS 인프라 정의, AWS 중심이면 권장 |
| 장점 | 환경 재현성 확보, 인프라 변경 이력 관리 |

---

## 우선순위 요약

| 순위 | 기술 | 이유 |
|---|---|---|
| 1 | **Testcontainers** | 학습 비용 낮고 테스트 품질 즉시 향상 |
| 2 | **OpenTelemetry** | 기존 관측성 스택 정리, 실무 표준 |
| 3 | **Kubernetes + Helm** | 인프라 레벨 업, 취업 시장 수요 가장 높음 |
| 4 | **gRPC** | 서비스 간 통신 고도화, K8s 환경과 시너지 |
| 5 | **pgvector + RAG** | AI 기능 차별화, 기존 PostgreSQL 재활용 |
| 6 | **ArgoCD** | K8s 도입 후 GitOps 완성 |
| 7 | **Terraform / AWS CDK** | 인프라 코드화로 환경 재현성 확보 |

> 취업을 목표로 한다면 **Kubernetes + ArgoCD** 조합을 먼저.
> AI 기능 차별화를 원한다면 **pgvector + RAG**를 먼저.

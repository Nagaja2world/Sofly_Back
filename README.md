# Sofly BE

> AI 기반 그룹 여행 플래닝 서비스 **Sofly**의 백엔드 레포지토리입니다.

| <img src="https://avatars.githubusercontent.com/u/88922405?v=4" width="100" height="100" alt="박상민 프로필"> | <img src="https://avatars.githubusercontent.com/u/168955357?v=4" width="100" height="100" alt="정세현 프로필"> |
|:---:|:---:|
| [박상민](https://github.com/sm010422) | [정세현](https://github.com/gitIt-sehyeon) |
| Backend | Backend |

---

## 프로젝트 소개

Sofly는 그룹 여행을 함께 계획할 수 있는 협업 여행 플래닝 플랫폼입니다.
AI 채팅을 통해 여행 일정을 추천받고, 항공권·호텔을 검색하며, 워크스페이스에서 팀원들과 실시간으로 일정을 공유할 수 있습니다.
여행 후에는 앨범과 여행 로그로 기억을 남길 수 있습니다.

**주요 기능**
- OAuth2 소셜 로그인 (Google, Kakao, Naver)
- 워크스페이스 기반 그룹 여행 계획 및 초대 코드 공유
- AI(Google Gemini) 여행 플래너 채팅 (3단계 맞춤 여행 추천)
- 일정표 생성 · 편집 · 포크(복제)
- 항공권 · 호텔 실시간 검색 (Amadeus API + Google Places API)
- Google Drive 연동 여행 앨범
- Markdown 기반 여행 로그 (공개/멤버/비공개 설정)

---

## 아키텍처

본 프로젝트는 **두 개의 독립 Spring Boot 서비스**로 구성된 멀티 모듈 구조입니다.

```
Sofly_Back/
├── services/
│   ├── travel-core-service/    # 핵심 비즈니스 로직 (Port 8080)
│   └── travel-supply-service/  # 항공·호텔 외부 API 연동 (Port 8081)
```

### travel-core-service

사용자 인증, 워크스페이스, 일정, AI 채팅, 앨범, 여행 로그 등 핵심 도메인을 담당합니다.
레이어드 아키텍처 기반으로 설계되었습니다.

### travel-supply-service

항공권·호텔 외부 공급자 API 연동을 담당합니다.
헥사고날 아키텍처(Ports & Adapters)를 적용하여 공급자 교체에 유연합니다.

---

## 기술 스택

<table>
  <tr>
    <td width="50%" valign="top">
      <b>Language & Framework</b><br><br>
      <img src="https://img.shields.io/badge/Java_21-007396?style=for-the-badge&logo=java&logoColor=white" />
      <img src="https://img.shields.io/badge/Spring_Boot_3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
      <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
      <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" />
      <img src="https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white" />
    </td>
    <td width="50%" valign="top">
      <b>Database & Cache</b><br><br>
      <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
      <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" />
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <b>Authentication</b><br><br>
      <img src="https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=jsonwebtokens&logoColor=white" />
      <img src="https://img.shields.io/badge/OAuth2-EB5424?style=for-the-badge&logo=auth0&logoColor=white" />
    </td>
    <td width="50%" valign="top">
      <b>AI</b><br><br>
      <img src="https://img.shields.io/badge/Spring_AI-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
      <img src="https://img.shields.io/badge/Google_Gemini-4285F4?style=for-the-badge&logo=google&logoColor=white" />
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <b>External API</b><br><br>
      <img src="https://img.shields.io/badge/Amadeus_API-1A1A2E?style=for-the-badge&logo=amadeus&logoColor=white" />
      <img src="https://img.shields.io/badge/Google_Places_API-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white" />
      <img src="https://img.shields.io/badge/Google_Drive_API-4285F4?style=for-the-badge&logo=googledrive&logoColor=white" />
    </td>
    <td width="50%" valign="top">
      <b>Infra & Deployment</b><br><br>
      <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
      <img src="https://img.shields.io/badge/AWS_Lightsail-FF9900?style=for-the-badge&logo=amazon-aws&logoColor=white" />
      <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white" />
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <b>CI/CD</b><br><br>
      <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" />
    </td>
    <td width="50%" valign="top">
      <b>Build & Test</b><br><br>
      <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" />
      <img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white" />
    </td>
  </tr>
</table>

---

## 주요 API

### travel-core-service (`:8080`)

| 분류 | Method | Endpoint | 설명 |
|------|--------|----------|------|
| 인증 | `POST` | `/api/auth/refresh` | 액세스 토큰 재발급 |
| 인증 | `POST` | `/api/auth/logout` | 로그아웃 |
| 일정 | `GET` | `/api/v1/schedules` | 워크스페이스 일정 목록 조회 |
| 일정 | `POST` | `/api/v1/schedules` | 일정 생성 |
| 일정 | `POST` | `/api/v1/schedules/{scheduleId}/fork` | 일정 복제 |
| 일정 | `POST` | `/api/v1/schedules/{scheduleId}/items` | 일정 아이템 추가 |
| AI 채팅 | `POST` | `/api/v1/chat/rooms` | 채팅방 생성 |
| AI 채팅 | `POST` | `/api/v1/chat/rooms/{roomId}` | AI에게 메시지 전송 |
| AI 채팅 | `GET` | `/api/v1/chat/rooms/{roomId}/messages` | 대화 내역 조회 |

API 문서: `GET /core-docs` (Swagger UI)

### travel-supply-service (`:8081`)

| 분류 | Method | Endpoint | 설명 |
|------|--------|----------|------|
| 항공권 | `GET` | `/supply/flights/offers` | 항공권 검색 |
| 호텔 | `GET` | `/supply/hotels/offers` | 호텔 검색 |
| 호텔 | `GET` | `/supply/hotels/place-info` | 호텔 상세 정보 (Google Places) |

API 문서: `GET /swagger-ui` (Swagger UI)

---

## 로컬 실행

```bash
# 전체 서비스 실행 (Docker Compose)
docker compose up -d

# Core Service 단독 실행
./gradlew :services:travel-core-service:bootRun

# Supply Service 단독 실행
./gradlew :services:travel-supply-service:bootRun
```

필요한 환경 변수는 `.env` 파일 또는 환경변수로 주입합니다. (`.env.example` 참고)

# Sofly BE

> AI 기반 그룹 여행 플래닝 서비스 Sofly의 백엔드 레포지토리

[![License](https://img.shields.io/badge/license-MIT-blue.svg)]()
[![Version](https://img.shields.io/badge/version-1.0.0-green.svg)]()

## 📋 Table of Contents

1. [Demo](#demo)
2. [Features](#features)
3. [Tech Stack](#tech-stack)
4. [Getting Started](#getting-started)
   - [Prerequisites](#prerequisites)
   - [Installation](#installation)
5. [Environment Variables](#environment-variables)
6. [Project Structure](#project-structure)
7. [Git Convention](#git-convention)
8. [Running Tests](#running-tests)
9. [API Reference](#api-reference)
10. [Screenshots](#screenshots)
11. [Team Members](#team-members)
12. [Contributing](#contributing)
13. [License](#license)

---

## 🎬 Demo

## ✨ Features

- OAuth2 소셜 로그인 (Google, Kakao, Naver)
- 워크스페이스 기반 그룹 여행 계획 및 초대 코드 공유
- AI(Google Gemini) 여행 플래너 채팅 — 3단계 맞춤 여행 추천
- 일정표 생성 · 편집 · 포크(복제)
- 항공권 · 호텔 실시간 검색 (Amadeus API + Google Places API)
- Google Drive 연동 여행 앨범
- Markdown 기반 여행 로그 (공개 / 멤버 / 비공개 설정)

---

## 🛠 Tech Stack

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
      <img src="https://img.shields.io/badge/Amadeus_API-1A1A2E?style=for-the-badge&logoColor=white" />
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

## 🚀 Getting Started

### Prerequisites

- Java 21
- Docker & Docker Compose
- `.env` 파일 (환경변수 설정 필요)

### Installation

```bash
# 레포지토리 클론
git clone https://github.com/your-org/Sofly_Back.git
cd Sofly_Back

# 전체 서비스 실행
docker compose up -d

# 개별 서비스 실행
./gradlew :services:travel-core-service:bootRun
./gradlew :services:travel-supply-service:bootRun
```

---

## ⚙️ Environment Variables

---

## 📁 Project Structure

```
Sofly_Back/
├── services/
│   ├── travel-core-service/    # 핵심 비즈니스 로직 (Port 8080)
│   │   └── src/main/java/com/sofly/core/
│   │       ├── auth/           # 인증 (OAuth2, JWT)
│   │       ├── workspace/      # 워크스페이스
│   │       ├── schedule/       # 여행 일정
│   │       ├── chat/           # AI 채팅
│   │       ├── album/          # 여행 앨범
│   │       └── travellog/      # 여행 로그
│   └── travel-supply-service/  # 항공·호텔 외부 API 연동 (Port 8081)
│       └── src/main/java/com/sofly/supply/
│           ├── flight/         # 항공권 검색 (Amadeus)
│           └── hotel/          # 호텔 검색 (Amadeus + Google Places)
```

---

## 🌿 Git Convention

### Branch Strategy

Git Flow 전략을 기반으로 운영합니다.

| 브랜치 | 설명 |
|--------|------|
| `main` | 배포 가능한 상태 (Production) |
| `develop` | 다음 배포를 위한 통합 브랜치 |
| `feat/이슈번호-기능명` | 기능 개발 (`feat/12-social-login`) |
| `fix/이슈번호-버그명` | 버그 수정 (`fix/34-websocket-error`) |
| `hotfix/이슈번호-버그명` | 긴급 버그 수정 (main에서 분기) |

### Commit Message

```
type(scope): subject
```

| 태그 | 설명 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 (기능 변화 없음) |
| `chore` | 빌드 설정, 패키지 관리 등 |
| `docs` | 문서 수정 |
| `style` | 코드 포맷팅 (비즈니스 로직 변경 없음) |
| `test` | 테스트 코드 추가/수정/삭제 |
| `ci` | CI 구성 파일 및 스크립트 변경 |
| `perf` | 성능 개선 |

---

## 🧪 Running Tests

```bash
./gradlew test
```

---

## 📡 API Reference

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

Swagger UI: `GET /core-docs`

### travel-supply-service (`:8081`)

| 분류 | Method | Endpoint | 설명 |
|------|--------|----------|------|
| 항공권 | `GET` | `/supply/flights/offers` | 항공권 검색 |
| 호텔 | `GET` | `/supply/hotels/offers` | 호텔 검색 |
| 호텔 | `GET` | `/supply/hotels/place-info` | 호텔 상세 정보 (Google Places) |

Swagger UI: `GET /swagger-ui`

---

## 📸 Screenshots

---

## 👥 Team Members

| <img src="https://avatars.githubusercontent.com/u/88922405?v=4" width="100" height="100" alt="박상민 프로필"> | <img src="https://avatars.githubusercontent.com/u/168955357?v=4" width="100" height="100" alt="정세현 프로필"> |
|:---:|:---:|
| [박상민](https://github.com/sm010422) | [정세현](https://github.com/gitIt-sehyeon) |
| Backend | Backend |

---

## 🤝 Contributing

---

## 📄 License

# sofly-core-service Entity 설계 문서

## 개요

`sofly-core-service`의 JPA 엔티티 계층 설계를 정리한 문서입니다.
모든 엔티티는 `BaseTimeEntity`를 상속받아 `createdAt`, `updatedAt`을 공통으로 가집니다.

---

## 패키지 구조

```
src/main/java/com/sofly/core
├── global
│   └── entity
│       └── BaseTimeEntity.java
└── domain
    ├── user
    │   └── entity
    │       └── User.java
    ├── workspace
    │   └── entity
    │       ├── Workspace.java
    │       ├── WorkspaceMember.java
    │       └── SavedFlight.java
    ├── schedule
    │   └── entity
    │       ├── Schedule.java
    │       ├── ScheduleItem.java
    │       └── AiChatMessage.java
    ├── album
    │   └── entity
    │       ├── Album.java
    │       └── Photo.java
    └── travellog
        └── entity
            └── TravelLog.java
```

---

## 엔티티 관계도

```
User (1) ──────────────────────────────────── (N) WorkspaceMember
 │                                                       │
 │                                              (N) Workspace (1)
 │                                                       │
 │                                    ┌──────────────────┼──────────────────┐
 │                                    │                  │                  │
 │                                  (N) SavedFlight   (N) Schedule       (1) Album
 │                                                       │                  │
 │                                                   (N) ScheduleItem    (N) Photo
 │                                                   (N) AiChatMessage
 │
 └──── (N) TravelLog
```

---

## BaseTimeEntity

**테이블:** 별도 테이블 없음 (`@MappedSuperclass`)
**경로:** `global/entity/BaseTimeEntity.java`

모든 엔티티가 상속받는 공통 추상 클래스입니다.
`@EnableJpaAuditing`이 메인 클래스에 선언되어 있어야 동작합니다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `createdAt` | `LocalDateTime` | 생성 일시 (자동, 수정 불가) |
| `updatedAt` | `LocalDateTime` | 수정 일시 (자동) |

---

## User

**테이블:** `users`
**경로:** `domain/user/entity/User.java`

OAuth2 소셜 로그인 기반 사용자 정보와 여행 성향 프로필을 관리합니다.

### 주요 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Long` | Y | PK (auto increment) |
| `email` | `String` | Y | 이메일 (unique) |
| `nickname` | `String` | Y | 닉네임 |
| `profileImageUrl` | `String` | N | 프로필 이미지 URL |
| `provider` | `Provider` | Y | 소셜 로그인 제공자 |
| `providerId` | `String` | Y | OAuth2 고유 ID |
| `role` | `Role` | Y | 권한 (기본값: USER) |
| `ageGroup` | `AgeGroup` | N | 연령대 |
| `city` | `String` | N | 거주 도시 |
| `preferCompanionType` | `CompanionType` | N | 선호 동행 타입 |
| `budgetMin` | `Integer` | N | 선호 예산 최솟값 (원) |
| `budgetMax` | `Integer` | N | 선호 예산 최댓값 (원) |
| `preferThemes` | `List<TravelTheme>` | N | 선호 테마 목록 (`user_prefer_themes` 별도 테이블) |
| `preferCities` | `List<String>` | N | 선호 도시 태그 목록 (`user_prefer_cities` 별도 테이블) |

### Enum

| Enum | 값 |
|------|----|
| `Provider` | `GOOGLE`, `KAKAO` |
| `Role` | `USER`, `ADMIN` |
| `AgeGroup` | `TEENS`, `TWENTIES`, `THIRTIES`, `FORTIES_PLUS` |
| `CompanionType` | `SOLO`, `COUPLE`, `FRIENDS`, `FAMILY` |
| `TravelTheme` | `RELAXATION`, `SHOPPING`, `CULTURE`, `ACTIVITY`, `FOOD` |

### 컬렉션 테이블

| 테이블명 | FK | 컬럼 | 설명 |
|----------|----|------|------|
| `user_prefer_themes` | `user_id` | `theme` | 선호 테마 (다중 선택) |
| `user_prefer_cities` | `user_id` | `city_name` | 선호 도시 태그 |

---

## Workspace

**테이블:** `workspaces`
**경로:** `domain/workspace/entity/Workspace.java`

여행 단위 공간. 항공편, 일정, 앨범, 여행기의 루트 엔티티입니다.

### 주요 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Long` | Y | PK |
| `title` | `String` | Y | 여행 제목 |
| `destination` | `String` | Y | 목적지 (도시/국가명) |
| `countryCode` | `String` | N | ISO 3166-1 alpha-2 코드 (정복도용, 예: `JP`) |
| `startDate` | `LocalDate` | Y | 여행 시작일 |
| `endDate` | `LocalDate` | Y | 여행 종료일 |
| `headcount` | `Integer` | N | 동행 인원 |
| `coverImageUrl` | `String` | N | 대표 이미지 URL |
| `inviteCode` | `String` | N | 초대 링크 코드 (UUID) |
| `owner` | `User` | Y | 소유자 (FK: `owner_id`) |

### 연관 관계

| 관계 | 대상 | 설명 |
|------|------|------|
| `@ManyToOne` | `User` | 소유자 |
| `@OneToMany` | `WorkspaceMember` | 멤버 목록 |

---

## WorkspaceMember

**테이블:** `workspace_members`
**경로:** `domain/workspace/entity/WorkspaceMember.java`

`Workspace`와 `User`의 N:M 조인 엔티티. 권한 정보를 포함합니다.
`(workspace_id, user_id)` unique 제약으로 중복 가입을 방지합니다.

### 주요 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Long` | Y | PK |
| `workspace` | `Workspace` | Y | FK: `workspace_id` |
| `user` | `User` | Y | FK: `user_id` |
| `role` | `MemberRole` | Y | 권한 (기본값: VIEWER) |

### Enum

| Enum | 값 | 설명 |
|------|----|------|
| `MemberRole` | `OWNER` | 소유자 |
| | `EDITOR` | 일정 수정 가능 |
| | `VIEWER` | 조회만 가능 |

### 팩토리 메서드

```java
WorkspaceMember.ofOwner(workspace, user)   // OWNER 권한으로 생성
WorkspaceMember.ofViewer(workspace, user)  // VIEWER 권한으로 생성
```

---

## SavedFlight

**테이블:** `saved_flights`
**경로:** `domain/workspace/entity/SavedFlight.java`

`sofly-supply-service`에서 조회한 항공편을 워크스페이스에 저장합니다.
supply 서비스의 응답을 역정규화(denormalize)해서 저장하므로 외래키 없이 값을 복사합니다.

### 주요 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Long` | Y | PK |
| `workspace` | `Workspace` | Y | FK: `workspace_id` |
| `flightNumber` | `String` | Y | 항공편 번호 (예: `KE123`) |
| `airline` | `String` | Y | 항공사명 |
| `departureAirport` | `String` | Y | 출발 공항 코드 (예: `ICN`) |
| `arrivalAirport` | `String` | Y | 도착 공항 코드 (예: `NRT`) |
| `departureTime` | `LocalDateTime` | Y | 출발 일시 |
| `arrivalTime` | `LocalDateTime` | Y | 도착 일시 |
| `duration` | `String` | N | 소요 시간 (예: `2h 30m`) |
| `price` | `Integer` | N | 참고용 가격 (원) |
| `flightType` | `FlightType` | Y | 출발/귀국 구분 (기본값: OUTBOUND) |

### Enum

| Enum | 값 |
|------|----|
| `FlightType` | `OUTBOUND` (출발), `RETURN` (귀국) |

---

## Schedule

**테이블:** `schedules`
**경로:** `domain/schedule/entity/Schedule.java`

AI가 생성한 여행 일정의 버전 단위 엔티티입니다.
하나의 워크스페이스에 여러 버전의 일정을 저장할 수 있습니다.

### 주요 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Long` | Y | PK |
| `workspace` | `Workspace` | Y | FK: `workspace_id` |
| `title` | `String` | N | 버전 제목 (예: `1차 생성`, `수정본`) |
| `version` | `Integer` | Y | 버전 번호 |
| `aiChatSessionId` | `String` | N | 연결된 AI 채팅 세션 ID |

### 연관 관계

| 관계 | 대상 | 설명 |
|------|------|------|
| `@OneToMany` | `ScheduleItem` | 일정 항목 목록 (`day ASC, orderIndex ASC` 정렬) |

---

## ScheduleItem

**테이블:** `schedule_items`
**경로:** `domain/schedule/entity/ScheduleItem.java`

Day별 여행 장소/활동 카드입니다. 드래그 앤 드롭 순서 변경을 `orderIndex`로 관리합니다.

### 주요 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Long` | Y | PK |
| `schedule` | `Schedule` | Y | FK: `schedule_id` |
| `day` | `Integer` | Y | 여행 일차 (1부터 시작) |
| `orderIndex` | `Integer` | Y | 일차 내 순서 (D&D로 변경) |
| `visitTime` | `String` | N | 방문 시간 (`HH:mm`) |
| `category` | `Category` | Y | 카테고리 |
| `name` | `String` | Y | 장소/활동 이름 |
| `address` | `String` | N | 주소 |
| `latitude` | `Double` | N | 위도 |
| `longitude` | `Double` | N | 경도 |
| `memo` | `String` | N | 메모 (TEXT) |
| `deepLinkUrl` | `String` | N | 예약 딥링크 URL |
| `estimatedCost` | `Integer` | N | 예상 비용 (원) |
| `deepLinkClickCount` | `Integer` | N | 딥링크 클릭 수 (통계용) |

### Enum

| Enum | 값 |
|------|----|
| `Category` | `ACCOMMODATION`, `RESTAURANT`, `CAFE`, `ATTRACTION`, `TRANSPORT` |

---

## AiChatMessage

**테이블:** `ai_chat_messages`
**경로:** `domain/schedule/entity/AiChatMessage.java`

AI 일정 생성 채팅 히스토리를 저장합니다.
`sessionId`로 하나의 대화 흐름을 묶어 관리합니다.

### 주요 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Long` | Y | PK |
| `sessionId` | `String` | Y | 채팅 세션 묶음 ID |
| `workspace` | `Workspace` | Y | FK: `workspace_id` |
| `role` | `Role` | Y | 발화자 구분 |
| `content` | `String` | Y | 메시지 내용 (TEXT) |

### Enum

| Enum | 값 |
|------|----|
| `Role` | `USER`, `ASSISTANT` |

---

## Album

**테이블:** `albums`
**경로:** `domain/album/entity/Album.java`

워크스페이스당 1개의 공유 앨범. Google Drive 폴더와 연동됩니다.

### 주요 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Long` | Y | PK |
| `workspace` | `Workspace` | Y | FK: `workspace_id` (unique — 1:1) |
| `driveFolderId` | `String` | N | 연동된 Google Drive 폴더 ID |
| `driveFolderName` | `String` | N | Drive 폴더 이름 |

### 연관 관계

| 관계 | 대상 | 설명 |
|------|------|------|
| `@OneToOne` | `Workspace` | 워크스페이스당 1개 |
| `@OneToMany` | `Photo` | 사진 목록 |

---

## Photo

**테이블:** `photos`
**경로:** `domain/album/entity/Photo.java`

앨범에 속하는 사진 메타데이터입니다.
실제 파일은 Google Drive에 저장되고, 서비스는 URL과 EXIF 정보만 관리합니다.

### 주요 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Long` | Y | PK |
| `album` | `Album` | Y | FK: `album_id` |
| `uploadedBy` | `User` | Y | FK: `uploaded_by` |
| `thumbnailUrl` | `String` | Y | 썸네일 URL |
| `originalUrl` | `String` | Y | 원본 URL (Drive 링크) |
| `driveFileId` | `String` | N | Google Drive 파일 ID |
| `takenAt` | `LocalDate` | N | 촬영 날짜 (EXIF) |
| `latitude` | `Double` | N | 촬영 위치 위도 (EXIF) |
| `longitude` | `Double` | N | 촬영 위치 경도 (EXIF) |
| `matchedDay` | `Integer` | N | EXIF 기반 매칭된 여행 일차 |

---

## TravelLog

**테이블:** `travel_logs`
**경로:** `domain/travellog/entity/TravelLog.java`

여행 후 작성하는 블로그형 여행기입니다.
공개 범위를 `Visibility`로 제어합니다.

### 주요 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Long` | Y | PK |
| `workspace` | `Workspace` | Y | FK: `workspace_id` |
| `author` | `User` | Y | FK: `author_id` |
| `title` | `String` | Y | 제목 |
| `content` | `String` | Y | 본문 (Markdown, TEXT) |
| `coverImageUrl` | `String` | N | 대표 이미지 URL |
| `visibility` | `Visibility` | Y | 공개 범위 (기본값: PRIVATE) |

### Enum

| Enum | 값 | 설명 |
|------|----|------|
| `Visibility` | `PRIVATE` | 본인만 |
| | `MEMBERS` | 워크스페이스 멤버 공개 |
| | `PUBLIC` | 전체 공개 (피드 노출) |

---

## 설계 원칙

**역정규화 (SavedFlight)**
`SavedFlight`는 `sofly-supply-service`에서 받아온 항공편 데이터를 그대로 복사해 저장합니다.
supply 서비스의 외부 API 결과를 직접 참조하는 외래키 구조 대신 값을 복사하는 방식을 채택해
서비스 간 결합도를 낮춥니다.

**비즈니스 메서드**
Setter를 열지 않고 엔티티에 의미 있는 메서드명으로 상태 변경을 캡슐화했습니다.
예) `workspace.generateInviteCode()`, `scheduleItem.incrementDeepLinkClick()`

**Lazy Loading**
모든 연관 관계는 `FetchType.LAZY`로 설정해 불필요한 쿼리를 방지합니다.
N+1 문제는 Service 계층에서 `@EntityGraph` 또는 fetch join으로 해결합니다.

**ddl-auto 전략**
- 로컬 개발: `update`
- 운영 배포: `validate` (스키마 변경은 반드시 마이그레이션 스크립트로 관리)

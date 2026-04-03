# FR-002 사용자 프로필 관리 구현 문서

## 개요

사용자가 마이페이지에서 기본 정보 및 여행 성향을 등록·수정할 수 있는 API를 구현한다.
등록된 프로필 정보는 AI 일정 생성 시 기본 조건으로 자동 반영된다.

---

## 디렉토리 구조

```
domain/user/
  ├── code/
  │   └── UserErrorCode.java
  ├── controller/
  │   └── UserController.java
  ├── dto/
  │   ├── UserProfileResponse.java
  │   └── UserProfileUpdateRequest.java
  ├── entity/
  │   └── User.java
  ├── exception/
  │   └── UserException.java
  ├── repository/
  │   └── UserRepository.java
  └── service/
      └── UserService.java

global/security/util/
  └── SecurityUtils.java
```

---

## API 명세

### 프로필 조회

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| URL | `/api/users/me/profile` |
| 인증 | Bearer Token 필요 |

**Response**
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "여행자",
  "profileImageUrl": "https://...",
  "ageGroup": "TWENTIES",
  "city": "서울",
  "preferCompanionType": "COUPLE",
  "budgetMin": 500000,
  "budgetMax": 1500000,
  "preferThemes": ["RELAXATION", "FOOD"],
  "preferCities": ["제주", "부산"],
  "profileCompleted": true
}
```

---

### 프로필 등록/수정

| 항목 | 내용 |
|------|------|
| Method | `PATCH` |
| URL | `/api/users/me/profile` |
| 인증 | Bearer Token 필요 |

**Request Body**
```json
{
  "nickname": "여행자",
  "city": "서울",
  "ageGroup": "TWENTIES",
  "preferCompanionType": "COUPLE",
  "budgetMin": 500000,
  "budgetMax": 1500000,
  "preferThemes": ["RELAXATION", "FOOD"],
  "preferCities": ["제주", "부산"]
}
```

**Response** - 조회 응답과 동일

---

## Enum 값 목록

| 필드 | 값 |
|------|----|
| `ageGroup` | `TEENS`, `TWENTIES`, `THIRTIES`, `FORTIES_PLUS` |
| `preferCompanionType` | `SOLO`, `COUPLE`, `FRIENDS`, `FAMILY` |
| `preferThemes` | `RELAXATION`, `SHOPPING`, `CULTURE`, `ACTIVITY`, `FOOD` |

---

## 주요 파일 설명

### `UserController`
- `SecurityUtils.getCurrentUserId()`로 JWT에서 userId 추출
- `@Valid`로 요청 DTO 검증

### `UserService`
- `getMyProfile(userId)` — 프로필 조회
- `updateMyProfile(userId, request)` — 프로필 등록/수정
- `getProfileForAiSchedule(userId)` — AI 일정 생성 연동용 인터페이스

### `UserProfileResponse`
- `profileCompleted` 필드 포함
- `ageGroup`, `preferCompanionType`, `budgetMin`, `budgetMax`, `preferThemes` 모두 입력 시 `true`
- AI 서비스에서 프로필 완성 여부 체크 시 활용

### `UserException`
- `GeneralException` 상속
- `UserErrorCode`를 받아 도메인 전용 예외 생성
- `AuthException`과 동일한 패턴으로 도메인별 예외 분리

### `SecurityUtils`
- `SecurityContextHolder`에서 userId(Long) 직접 추출
- `JwtAuthenticationFilter`가 principal에 userId를 저장하는 구조 기반
- DB 조회 없이 바로 userId 반환

---

## 인증 흐름

```
클라이언트 요청 (Bearer Token)
        ↓
JwtAuthenticationFilter
  └── JWT 검증 → userId 추출 → SecurityContext에 저장
        ↓
UserController
  └── SecurityUtils.getCurrentUserId()
        ↓
UserService
  └── userRepository.findById(userId)
```

---

## Validation 규칙

| 필드 | 규칙 |
|------|------|
| `nickname` | 필수, 최대 20자 |
| `ageGroup` | 필수 |
| `preferCompanionType` | 필수 |
| `budgetMin`, `budgetMax` | 0 이상 |
| `preferThemes` | 최대 5개 |
| `preferCities` | 최대 10개 |

---

## AI 일정 생성 연동

`UserService.getProfileForAiSchedule(userId)`를 통해 AI 일정 서비스에서 사용자 프로필을 조회한다.
`profileCompleted: false`인 경우 AI 서비스에서 프로필 입력을 유도하는 처리가 필요하다.


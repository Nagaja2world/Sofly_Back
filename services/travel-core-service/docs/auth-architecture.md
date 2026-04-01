# 인증/보안 아키텍처 문서

> `global/auth` 및 `global/security` 패키지에 속한 파일들의 역할과 흐름을 정리한 문서입니다.

---

## 전체 흐름 요약

```
[소셜 로그인 (Google/Kakao/Naver)]
        ↓
CustomOAuth2UserService       ← OAuth2 사용자 정보 수신 및 DB 저장/갱신
        ↓
OAuth2AuthenticationSuccessHandler  ← JWT 발급 + Redis에 Refresh Token 저장
        ↓
클라이언트 (프론트엔드로 토큰과 함께 리다이렉트)

[이후 API 요청]
        ↓
JwtAuthenticationFilter       ← Bearer 토큰 검증 → SecurityContext 설정
        ↓
컨트롤러 (인증된 사용자로 요청 처리)

[토큰 만료 시]
        ↓
POST /api/auth/refresh         ← Refresh Token으로 Access Token 재발급
        ↓
AuthService                    ← Redis에서 Refresh Token 검증 + 토큰 로테이션
```

---

## `global/auth` 패키지

인증 관련 HTTP 엔드포인트, 비즈니스 로직, DTO, 예외 코드를 담당합니다.

---

### `AuthController.java`

**역할:** 토큰 재발급 및 로그아웃 HTTP 엔드포인트 제공

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/api/auth/refresh` | Refresh Token으로 새 Access/Refresh Token 발급 |
| `POST` | `/api/auth/logout` | Redis에서 Refresh Token 삭제 (로그아웃 처리) |

---

### `AuthService.java`

**역할:** 토큰 재발급(Token Rotation) 및 로그아웃 비즈니스 로직

- **토큰 재발급 흐름:**
  1. 클라이언트가 보낸 Refresh Token을 `JwtTokenProvider`로 검증
  2. Redis에서 해당 userId의 Refresh Token과 일치 여부 확인
  3. 새 Access Token + Refresh Token 발급 (Token Rotation — 보안 강화)
  4. Redis의 Refresh Token을 새 토큰으로 교체
- **로그아웃:** Redis에서 해당 userId의 Refresh Token 삭제

---

### `RefreshTokenRequest.java`

**역할:** `/api/auth/refresh` 요청 DTO

```java
record RefreshTokenRequest(
    @NotBlank String refreshToken
)
```

---

### `TokenResponse.java`

**역할:** 토큰 재발급 응답 DTO

```java
record TokenResponse(
    String accessToken,
    String refreshToken
)
```

팩토리 메서드 `TokenResponse.of(accessToken, refreshToken)`으로 생성합니다.

---

### `AuthErrorCode.java`

**역할:** 인증 관련 에러 코드 정의 (enum)

| 코드 | 설명 |
|------|------|
| `INCORRECT_PASSWORD` | 비밀번호 불일치 |
| `TOKEN_EXPIRED` | 액세스 토큰 만료 |
| `INVALID_TOKEN` | 유효하지 않은 토큰 |
| `INVALID_REFRESH_TOKEN` | 유효하지 않은 Refresh Token |
| `REFRESH_TOKEN_EXPIRED` | Refresh Token 만료 |
| `DUPLICATE_LOGIN_ID` | 중복된 로그인 ID |
| `EMPTY_AUTHENTICATION` | 인증 정보 없음 |
| `MEMBER_NOT_FOUND` | 사용자 없음 |

---

### `AuthSuccessCode.java`

**역할:** 인증 관련 성공 코드 정의 (enum)

| 코드 | 설명 |
|------|------|
| `LOGIN_SUCCESS` | 로그인 성공 |
| `SIGNUP_SUCCESS` | 회원가입 성공 |
| `LOGOUT_SUCCESS` | 로그아웃 성공 |
| `REISSUE_SUCCESS` | 토큰 재발급 성공 |

---

### `AuthException.java`

**역할:** 인증 도메인 전용 커스텀 예외

`GeneralException`을 상속하며, `AuthErrorCode`를 받아 예외를 생성합니다.

---

### `CustomUserDetailsService.java`

**역할:** (현재 미사용) UserDetailsService 레거시 구현체 자리

현재 모든 코드가 주석 처리되어 있으며, 소셜 로그인 기반 OAuth2 방식을 사용하므로 실제로는 동작하지 않습니다.

---

## `global/security` 패키지

JWT 처리, OAuth2 소셜 로그인 연동, Spring Security 설정을 담당합니다.

---

### `SecurityConfig.java`

**역할:** Spring Security 전체 보안 정책 설정

핵심 설정 내용:
- **세션 비활성화:** `STATELESS` 정책으로 JWT 기반 무상태 인증
- **OAuth2 로그인:** `/oauth2/authorization/**` 진입점, 성공/실패 핸들러 연결
- **JWT 필터 등록:** `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 위치
- **공개 엔드포인트:** `/api/auth/**`, `/oauth2/**`, Swagger(`/core-docs/**`), 헬스체크는 인증 없이 허용
- **그 외 모든 요청:** 인증 필요 (미인증 시 401 반환)

---

### `JwtAuthenticationFilter.java`

**역할:** 모든 HTTP 요청에서 JWT를 검증하고 SecurityContext를 설정하는 필터

**처리 흐름:**
1. `Authorization: Bearer <token>` 헤더에서 토큰 추출
2. `JwtTokenProvider`로 토큰 유효성 검증
3. 유효하면 userId와 `ROLE_USER` 권한으로 `UsernamePasswordAuthenticationToken` 생성
4. `SecurityContextHolder`에 인증 정보 저장

공개 경로(auth, oauth2, swagger)는 필터 자체를 통과시킵니다.

---

### `JwtTokenProvider.java`

**역할:** JWT 생성, 검증, 파싱의 핵심 컴포넌트

- **`generateAccessToken(userId)`** — 짧은 만료 시간의 Access Token 발급
- **`generateRefreshToken(userId)`** — 긴 만료 시간의 Refresh Token 발급
- **`validateToken(token)`** — 서명 및 만료 여부 검증 (boolean 반환)
- **`getUserId(token)`** — 토큰 클레임에서 userId 추출
- **서명 알고리즘:** HMAC-SHA (secret key 기반)

---

### `JwtProperties.java`

**역할:** JWT 관련 설정값을 `application-core.yaml`에서 바인딩하는 Properties 클래스

```yaml
# application-core.yaml 예시
jwt:
  secret-key: "..."
  access-token-expiration: 3600000    # 1시간 (ms)
  refresh-token-expiration: 604800000 # 7일 (ms)
```

`@ConfigurationProperties`로 타입 안전하게 주입됩니다.

---

### OAuth2 소셜 로그인 관련 파일들

#### `CustomOAuth2UserService.java`

**역할:** 소셜 로그인 성공 후 OAuth2 사용자 정보를 받아 처리하는 서비스

**처리 흐름:**
1. Spring OAuth2 Client가 소셜 플랫폼으로부터 사용자 정보 수신
2. provider(google/kakao/naver)에 따라 적절한 `OAuth2UserInfo` 구현체로 파싱
3. DB에서 해당 이메일의 User 조회
   - 없으면 신규 생성
   - 있으면 닉네임/프로필 이미지 갱신
4. `CustomOAuth2User` 객체 반환 (이후 핸들러에서 사용)

---

#### `CustomOAuth2User.java`

**역할:** Spring Security의 `OAuth2User` 인터페이스 구현체

`User` 엔티티를 감싸서 OAuth2 인증 객체로 변환합니다. `getUserId()` 메서드로 내부 userId에 접근할 수 있습니다.

---

#### `OAuth2UserInfo.java` (인터페이스)

**역할:** 소셜 플랫폼별 사용자 정보 추출의 공통 계약 정의

```java
interface OAuth2UserInfo {
    String getProviderId();       // 플랫폼 고유 ID
    String getEmail();
    String getNickname();
    String getProfileImageUrl();
}
```

---

#### `GoogleUserInfo.java` / `KakaoUserInfo.java` / `NaverUserInfo.java`

**역할:** 각 소셜 플랫폼의 응답 구조에 맞게 `OAuth2UserInfo`를 구현하는 클래스들

| 파일 | 특이사항 |
|------|---------|
| `GoogleUserInfo` | `sub` 필드를 providerId로 사용, 플랫 구조 |
| `KakaoUserInfo` | `kakao_account.profile` 중첩 구조에서 데이터 추출 |
| `NaverUserInfo` | `response` 중첩 객체에서 데이터 추출 |

---

#### `OAuth2AuthenticationSuccessHandler.java`

**역할:** OAuth2 로그인 성공 시 JWT 발급 및 프론트엔드 리다이렉트

**처리 흐름:**
1. `CustomOAuth2User`에서 userId 추출
2. Access Token + Refresh Token 발급 (`JwtTokenProvider`)
3. Refresh Token을 Redis에 TTL과 함께 저장 (`RefreshTokenRepository`)
4. 프론트엔드 URL로 토큰을 쿼리 파라미터로 붙여 리다이렉트

```
FRONTEND_REDIRECT_URL?accessToken=xxx&refreshToken=yyy
```

---

#### `OAuth2AuthenticationFailureHandler.java`

**역할:** OAuth2 로그인 실패 시 에러와 함께 프론트엔드로 리다이렉트

```
FRONTEND_REDIRECT_URL?error=<에러메시지>
```

---

### Redis 토큰 저장소

#### `RefreshToken.java`

**역할:** Redis에 저장되는 Refresh Token 엔티티

```java
@RedisHash("refresh_token")
class RefreshToken {
    @Id String userId;   // userId를 키로 사용
    String token;
    @TimeToLive long ttl; // 자동 만료
}
```

---

#### `RefreshTokenRepository.java`

**역할:** `RefreshToken` Redis CRUD 인터페이스

`CrudRepository<RefreshToken, String>`을 상속하여 `save`, `findById`, `deleteById` 등을 제공합니다.

---

### 유틸리티

#### `FilterResponseUtils.java`

**역할:** 필터 레이어에서 JSON 형태의 에러 응답을 직접 내려주는 유틸리티

일반적으로 Spring MVC의 `@ExceptionHandler`는 필터 단계에서는 동작하지 않습니다.
`JwtAuthenticationFilter`에서 인증 실패 시 이 유틸리티로 직접 `HttpServletResponse`에 JSON을 작성합니다.

---

#### `SwaggerConfig.java`

**역할:** Swagger(OpenAPI) 설정 + JWT Bearer 인증 스킴 추가

Swagger UI(`http://localhost:8080/core-docs`)에서 `Authorize` 버튼으로 JWT 토큰을 입력하면 인증이 필요한 API를 직접 테스트할 수 있습니다.

---

## 파일 의존 관계 요약

```
SecurityConfig
  ├── JwtAuthenticationFilter → JwtTokenProvider → JwtProperties
  ├── CustomOAuth2UserService → OAuth2UserInfo 구현체들 (Google/Kakao/Naver)
  │                          → CustomOAuth2User
  ├── OAuth2AuthenticationSuccessHandler → JwtTokenProvider
  │                                      → RefreshTokenRepository → RefreshToken
  └── OAuth2AuthenticationFailureHandler

AuthController → AuthService → JwtTokenProvider
                              → RefreshTokenRepository

FilterResponseUtils (JwtAuthenticationFilter에서 사용)
```

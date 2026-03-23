# Sofly Auth API 문서

> **Base URL** `http://localhost:8080`
> **인증 방식** JWT Bearer Token
> **Content-Type** `application/json`

---

## 공통 응답 형식

모든 API는 아래 형식으로 응답합니다.

```json
{
  "success": true,
  "message": null,
  "data": { ... }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | boolean | 요청 성공 여부 |
| `message` | string | 실패 시 에러 메시지 (성공 시 null) |
| `data` | object | 실제 응답 데이터 (실패 시 null) |

---

## 에러 코드

| HTTP | 에러 코드 | 설명 |
|------|-----------|------|
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| 401 | `INVALID_REFRESH_TOKEN` | 유효하지 않은 Refresh Token |
| 401 | `REFRESH_TOKEN_NOT_FOUND` | Redis에서 Refresh Token을 찾을 수 없음 |
| 400 | `INVALID_INPUT` | 요청 파라미터 유효성 오류 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

---

## 1. 소셜 로그인

### 로그인 시작

소셜 로그인은 Spring Security OAuth2가 처리합니다. 아래 URL로 **브라우저 리다이렉트**만 하면 됩니다.

| Provider | URL |
|----------|-----|
| Google | `GET /oauth2/authorization/google` |
| Kakao | `GET /oauth2/authorization/kakao` |
| Naver | `GET /oauth2/authorization/naver` |

```javascript
// 예시
window.location.href = 'http://localhost:8080/oauth2/authorization/google';
```

### 로그인 콜백

로그인 성공 시 설정된 `OAUTH2_REDIRECT_URI`로 리다이렉트됩니다.
쿼리 파라미터로 토큰이 전달됩니다.

```
http://localhost:3000/oauth2/callback
  ?accessToken=eyJhbGci...
  &refreshToken=eyJhbGci...
```

로그인 실패 시

```
http://localhost:3000/oauth2/callback
  ?error=에러메시지
```

#### 프론트 처리 예시

```javascript
// /oauth2/callback 페이지에서
const params = new URLSearchParams(window.location.search);

const accessToken  = params.get('accessToken');
const refreshToken = params.get('refreshToken');
const error        = params.get('error');

if (error) {
  console.error('로그인 실패:', error);
  return;
}

// 토큰 저장
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

// 메인 페이지로 이동
window.location.href = '/';
```

---

## 2. 토큰 재발급

Access Token이 만료되면 Refresh Token으로 재발급합니다.

```
POST /api/auth/refresh
```

**Request Body**

```json
{
  "refreshToken": "eyJhbGci..."
}
```

**Response** `200 OK`

```json
{
  "success": true,
  "message": null,
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci..."
  }
}
```

> Refresh Token Rotate 방식 — 재발급 시 Refresh Token도 새로 발급됩니다. 반드시 새 Refresh Token으로 교체하세요.

**Error Response** `401`

```json
{
  "success": false,
  "message": "유효하지 않은 Refresh Token입니다.",
  "data": null
}
```

#### 프론트 처리 예시 (Axios Interceptor)

```javascript
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');

      try {
        const { data } = await axios.post('/api/auth/refresh', { refreshToken });
        localStorage.setItem('accessToken',  data.data.accessToken);
        localStorage.setItem('refreshToken', data.data.refreshToken);

        // 원래 요청 재시도
        error.config.headers['Authorization'] = 'Bearer ' + data.data.accessToken;
        return axios(error.config);
      } catch {
        // Refresh Token도 만료 → 로그인 페이지로
        localStorage.clear();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

---

## 3. 로그아웃

```
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

**Response** `200 OK`

```json
{
  "success": true,
  "message": null,
  "data": null
}
```

#### 프론트 처리 예시

```javascript
async function logout() {
  await axios.post('/api/auth/logout', null, {
    headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
  });

  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  window.location.href = '/login';
}
```

---

## 4. 인증이 필요한 API 요청

모든 인증 API는 `Authorization` 헤더에 Access Token을 담아 보냅니다.

```
Authorization: Bearer eyJhbGci...
```

#### Axios 기본 설정 예시

```javascript
const api = axios.create({
  baseURL: 'http://localhost:8080',
});

api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});
```

---

## 5. Token 만료 시간

| 토큰 | 만료 시간 |
|------|-----------|
| Access Token | 4시간 |
| Refresh Token | 14일 |

JWT Payload 예시 (디코딩 후)

```json
{
  "sub": "1",
  "type": "access",
  "iat": 1700000000,
  "exp": 1700014400
}
```

| 필드 | 설명 |
|------|------|
| `sub` | userId |
| `type` | `access` or `refresh` |
| `iat` | 발급 시간 (Unix timestamp) |
| `exp` | 만료 시간 (Unix timestamp) |

---

## 6. 테스트 페이지

서버 실행 후 아래 URL에서 소셜 로그인을 직접 테스트할 수 있습니다.

| URL | 설명 |
|-----|------|
| `http://localhost:8080` | 로그인 테스트 UI |
| `http://localhost:8080/swagger-ui/core` | Swagger UI |
| `http://localhost:8080/v3/api-docs/core` | OpenAPI JSON |
| `http://localhost:8080/actuator/health` | 서버 헬스체크 |

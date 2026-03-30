# CI/CD 빌드 파이프라인 문서

## 개요

GitHub Actions + Docker Hub + 멀티스테이지 Dockerfile을 활용한 빌드 및 배포 파이프라인입니다.
빌드는 GitHub Actions 서버에서 처리하고, Lightsail 서버는 Docker Hub에서 이미지를 pull해서 실행만 해요.

---

## 전체 흐름

```
개발자 (develop 브랜치 push)
  ↓
GitHub Actions 서버 (ubuntu-latest)
  ├── 코드 clone
  ├── Docker 이미지 빌드 (멀티스테이지 Dockerfile)
  │     ├── 1단계: JDK + gradlew bootJar → JAR 생성
  │     └── 2단계: JAR만 복사 → 경량 실행 이미지
  └── Docker Hub push
        ↓
  sm010422/sofly-core:{github.sha}
        ↓
Lightsail 서버 (SSH)
  ├── docker pull
  ├── 새 컨테이너 실행 (Blue/Green)
  ├── 헬스체크
  ├── Nginx 트래픽 전환
  └── 이전 컨테이너 종료
```

---

## 왜 멀티스테이지 Dockerfile을 쓰냐

### 기존 방식 (비효율적)

```
GitHub Actions
  ├── JDK 설치
  ├── gradlew bootJar    ← 1번째 빌드
  └── docker build       ← Dockerfile 안에서 또 gradlew 실행 (2번째 빌드)
        └── docker push
```

JAR 빌드가 두 번 일어나서 시간 낭비예요.

### 현재 방식 (멀티스테이지)

```
GitHub Actions
  └── docker build       ← Dockerfile 안에서 빌드 한 번만
        └── docker push
```

GitHub Actions에서 JDK 설치, gradlew 빌드 step이 전부 필요 없어요.

---

## Dockerfile 설명

```dockerfile
# ── 1단계: 빌드 ──────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 의존성 파일 먼저 복사 (캐시 레이어 분리)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x ./gradlew && \
    ./gradlew dependencies --no-daemon   # 의존성만 먼저 캐시

# 소스 복사 후 빌드 (src 바뀔 때만 여기서부터 재실행)
COPY src src
RUN ./gradlew clean bootJar -x test --no-daemon

# ── 2단계: 실행 ──────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine      # JDK 아닌 JRE만 (경량)
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-Dspring.config.name=application-core", "-jar", "app.jar"]
```

### 캐시 레이어 동작 방식

```
첫 번째 빌드
  → 의존성 다운로드 (시간 걸림)
  → JAR 빌드

두 번째 빌드 (src만 변경)
  → 의존성 캐시 재사용 ✅
  → JAR 빌드만 재실행 (빠름)

두 번째 빌드 (build.gradle 변경)
  → 의존성부터 다시 다운로드
  → JAR 빌드
```

`build.gradle`이나 `gradle` 폴더가 바뀌지 않으면 의존성 레이어는 캐시를 써서
빌드 시간이 크게 단축돼요.

---

## GitHub Actions 워크플로우 설명

### 트리거 조건

```yaml
on:
  push:
    branches: [ "develop" ]
    paths:
      - 'services/travel-core-service/**'   # core 코드 변경
      - '.github/workflows/deploy-core.yml' # 배포 스크립트 변경
      - 'docker-compose.yml'                # docker 설정 변경
```

세 경로 중 하나라도 변경되면 실행돼요.

---

### build-and-push job

```yaml
jobs:
  build-and-push:
    runs-on: ubuntu-latest   # GitHub 클라우드 서버에서 실행
```

**Step별 설명**

| Step | 설명 |
|------|------|
| `Checkout` | GitHub Actions 서버에 리포 코드 clone |
| `Set image tag` | `github.sha`(커밋 해시)를 output으로 저장 |
| `Login to Docker Hub` | Docker Hub 인증 |
| `Build & Push` | Dockerfile 실행 → 이미지 빌드 → Docker Hub push |
| `PR 코멘트` | PR일 때 빌드 성공/실패 코멘트 자동 작성 |

**이미지 태그 규칙**

```
{DOCKERHUB_USERNAME}/sofly-core:{github.sha}

예시:
sm010422/sofly-core:abc1234def5678...
```

커밋마다 고유한 이미지가 만들어져요. 특정 버전으로 롤백할 때 유용해요.

---

### deploy job

```yaml
deploy:
  needs: build-and-push   # build-and-push 성공 후 실행
```

**Step별 설명**

| 단계 | 설명 |
|------|------|
| 1 | 배포 경로 이동 |
| 2 | 현재 nginx 포트 확인 → Blue/Green 타겟 결정 |
| 3 | 이전 이미지 태그 저장 (롤백용) |
| 4 | `.env.core` 파일 생성 |
| 5 | Docker Hub에서 새 이미지 pull |
| 6 | 새 컨테이너 실행 (green) |
| 7 | 헬스체크 (`/actuator/health` → 200 확인) |
| 8 | Nginx 트래픽 전환 (`nginx -s reload`) |
| 9 | 이전 컨테이너(blue) 종료 |
| 10 | 미사용 이미지 정리 |

---

## 롤백 전략

헬스체크 실패 시 자동으로 롤백이 실행돼요.

```bash
rollback() {
  # 1. Nginx를 이전 포트(CURRENT)로 복구
  echo "set $core_url http://127.0.0.1:${CURRENT_PORT};" | \
    sudo tee /etc/nginx/bluegreen/service-url-core.inc
  sudo nginx -s reload

  # 2. 실패한 새 컨테이너(TARGET) 정리
  sudo docker stop core-${TARGET_COLOR}
  sudo docker rm core-${TARGET_COLOR}

}
```

### 롤백 시나리오

```
정상 배포
  blue(8080) 서비스 중
  green(8081) 새 컨테이너 실행
  헬스체크 통과
  Nginx → green으로 전환
  blue 종료

롤백 발생
  blue(8080) 서비스 중
  green(8081) 새 컨테이너 실행
  헬스체크 실패
  Nginx → blue(8080)로 복구
  green 컨테이너 정리
  서비스 중단 없음
```

---

## 필요한 GitHub Secrets

### build-and-push job

| Secret | 설명 |
|--------|------|
| `DOCKERHUB_USERNAME` | Docker Hub 아이디 |
| `DOCKERHUB_TOKEN` | Docker Hub Access Token |

### deploy job

| Secret | 설명 |
|--------|------|
| `LIGHTSAIL_HOST` | 서버 IP |
| `LIGHTSAIL_USER` | SSH 유저 |
| `LIGHTSAIL_SSH_KEY` | SSH 개인키 |
| `DEPLOY_PATH` | 서버 배포 경로 |
| `CORE_DATASOURCE_URL` | PostgreSQL URL |
| `CORE_DATASOURCE_USERNAME` | DB 유저 |
| `CORE_DATASOURCE_PASSWORD` | DB 비밀번호 |
| `REDIS_HOST` | Redis 호스트 |
| `REDIS_PORT` | Redis 포트 |
| `JWT_SECRET_KEY` | JWT 서명 키 |
| `GOOGLE_CLIENT_ID` | Google OAuth2 |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 |
| `KAKAO_CLIENT_ID` | Kakao OAuth2 |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth2 |
| `NAVER_CLIENT_ID` | Naver OAuth2 |
| `NAVER_CLIENT_SECRET` | Naver OAuth2 |
| `OAUTH2_REDIRECT_URI` | 로그인 성공 redirect URI |
| `SUPPLY_SERVICE_URL` | supply 서비스 URL |
| `GEMINI_API_KEY` | Gemini API 키 |

---

## Docker Hub Access Token 발급

```
Docker Hub 로그인 (hub.docker.com)
  → 우측 상단 프로필
  → Account Settings
  → Security
  → New Access Token
  → 이름: sofly-github-actions
  → Access permissions: Read & Write
  → Generate
  → 발급된 토큰 복사 (창 닫으면 다시 못 봄)
```

GitHub Secrets에 등록:

```
DOCKERHUB_USERNAME = sm010422
DOCKERHUB_TOKEN    = 발급된 토큰
```

---

## 이전 방식 vs 현재 방식

| 항목 | 이전 | 현재 |
|------|------|------|
| 빌드 방식 | GitHub Actions gradlew + Docker 패키징 | 멀티스테이지 Dockerfile |
| 빌드 횟수 | 2번 (중복) | 1번 |
| JDK 설치 | GitHub Actions에 필요 | 불필요 |
| 빌드 속도 | 느림 | 빠름 |
| Dockerfile 복잡도 | 단순 | 약간 복잡 |
| Lightsail 서버 부하 | 없음 | 없음 |

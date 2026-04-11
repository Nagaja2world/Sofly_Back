# Nginx Blue/Green 무중단 배포 구성 가이드
> api.sofly.co.kr 서버 설정 기록 | 작성일: 2026년 3월 27일

---

## 1. 개요

api.sofly.co.kr 서버에서 Nginx를 이용한 Blue/Green 무중단 배포 구성 과정을 기록합니다.  
core-service와 supply-service 두 개의 Spring Boot 애플리케이션을 대상으로 합니다.

---

## 2. 최종 디렉토리 구조

```
/etc/nginx/
├── nginx.conf                    ← 메인 설정 (수정 없음)
├── conf.d/
│   └── sofly.conf               ← 메인 서버 설정 (새로 생성)
├── bluegreen/                   ← 포트 변수 파일 전용 디렉토리 (새로 생성)
│   ├── service-url-core.inc     ← core 현재 포트 변수
│   └── service-url-supply.inc   ← supply 현재 포트 변수
├── snippets/                    ← 기존 유지 (건드리지 않음)
│   ├── fastcgi-php.conf
│   └── snakeoil.conf
└── sites-available/
    ├── sofly                    ← 기존 파일 (백업용, 비활성화)
    └── default
```

---

## 3. 핵심 개념

### 3-1. conf.d vs sites-available 차이

| 디렉토리 | 역할 | 자동 로드 |
|---|---|---|
| `conf.d/` | 전역 설정 파일 보관 | ✅ `*.conf` 자동 로드 |
| `sites-available/` | 가상호스트 보관소 | ❌ 직접 include 필요 |
| `sites-enabled/` | 활성화된 사이트 (심볼릭 링크) | ✅ nginx.conf에서 include |

> `sites-available`은 심볼릭 링크(`sites-enabled`) 패턴을 쓸 때 의미가 있습니다.  
> `conf.d`에 `sofly.conf`를 두는 방식이 더 직관적입니다.

### 3-2. .inc 파일을 bluegreen/ 디렉토리에 두는 이유

- `conf.d/`에 `.conf` 확장자로 두면 → nginx가 자동 로드 시 `server{}` 블록 밖에서 `set` 지시어 오류 발생
- `snippets/`에 두면 → `nginx.conf`가 `snippets/*`를 자동 로드하여 동일한 오류 발생
- `bluegreen/`는 `nginx.conf`에서 자동 로드하지 않으므로 안전
- `sofly.conf` 내부 `server{}` 블록 안에서만 `include`하기 때문에 `set` 지시어가 정상 동작

### 3-3. set 지시어 위치 규칙

`set` 지시어는 반드시 `server{}` 또는 `location{}` 블록 안에서만 사용 가능합니다.

```nginx
# ❌ 잘못된 위치 (http 블록 최상위)
include /etc/nginx/bluegreen/service-url-core.inc;
server { ... }

# ✅ 올바른 위치 (server 블록 안)
server {
    include /etc/nginx/bluegreen/service-url-core.inc;
    ...
}
```

### 3-4. 파일 작성 시 홑따옴표 규칙

```bash
# ❌ 쌍따옴표 → 쉘이 $core_url을 빈 값으로 치환
echo "set $core_url http://127.0.0.1:8080;"

# ✅ 홑따옴표 → $가 그대로 파일에 저장됨
echo 'set $core_url http://127.0.0.1:8080;'
```

---

## 4. 설정 파일 내용

### 4-1. /etc/nginx/bluegreen/service-url-core.inc

```nginx
set $core_url http://127.0.0.1:8080;
```

### 4-2. /etc/nginx/bluegreen/service-url-supply.inc

```nginx
set $supply_url http://127.0.0.1:8082;
```

### 4-3. /etc/nginx/conf.d/sofly.conf

```nginx
# HTTP → HTTPS 리다이렉트
server {
    listen 80;
    server_name api.sofly.co.kr;
    if ($host = api.sofly.co.kr) {
        return 301 https://$host$request_uri;
    }
}

# HTTPS + Blue/Green 라우팅
server {
    listen 443 ssl;
    server_name api.sofly.co.kr;

    # include는 반드시 server 블록 안에 위치
    include /etc/nginx/bluegreen/service-url-core.inc;
    include /etc/nginx/bluegreen/service-url-supply.inc;

    # Certbot SSL 설정
    ssl_certificate /etc/letsencrypt/live/api.sofly.co.kr/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.sofly.co.kr/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    # core-service 라우팅
    location /api/ {
        proxy_pass $core_url;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    location /login/ {
        proxy_pass $core_url;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    location /oauth2/ {
        proxy_pass $core_url;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    location /swagger-ui/ {
        proxy_pass $core_url;
        proxy_set_header Host $host;
    }
    location /v3/api-docs/ {
        proxy_pass $core_url;
        proxy_set_header Host $host;
    }

    # supply-service 라우팅
    location /supply/ {
        proxy_pass $supply_url;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass $core_url;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 5. sites-enabled → conf.d 전환 절차

> `systemctl reload nginx`는 무중단입니다.  
> 새 worker가 새 설정으로 뜨고, 기존 worker는 처리 중인 요청을 마무리 후 종료합니다.

### Step 1. bluegreen 디렉토리 생성 및 .inc 파일 배치

```bash
sudo mkdir /etc/nginx/bluegreen
echo 'set $core_url http://127.0.0.1:8080;' | sudo tee /etc/nginx/bluegreen/service-url-core.inc
echo 'set $supply_url http://127.0.0.1:8082;' | sudo tee /etc/nginx/bluegreen/service-url-supply.inc
```

### Step 2. conf.d/sofly.conf 생성

```bash
sudo nano /etc/nginx/conf.d/sofly.conf
```

위 4-3의 내용 입력 (include를 server{} 블록 안에 위치시킬 것)

### Step 3. 기존 sites-enabled 심볼릭 링크 제거

```bash
sudo rm /etc/nginx/sites-enabled/sofly
```

> `sites-available/sofly` 파일은 삭제하지 않습니다. 심볼릭 링크만 제거하면 로드되지 않습니다.

### Step 4. 문법 검사 후 reload

```bash
sudo nginx -t
# nginx: configuration file /etc/nginx/nginx.conf syntax is ok
# nginx: configuration file /etc/nginx/nginx.conf test is successful

sudo systemctl reload nginx
```

### Step 5. 정상 동작 확인

```bash
# nginx 상태 확인
sudo systemctl status nginx

# 실제 요청 테스트 (200/401/403이면 정상, 502면 앱 미실행)
curl -I https://api.sofly.co.kr/api/

# 현재 라우팅 포트 확인
cat /etc/nginx/bluegreen/service-url-core.inc
cat /etc/nginx/bluegreen/service-url-supply.inc

# 앱 포트 실행 여부 확인
sudo ss -tlnp | grep -E '8080|8081|8082|8083'
```

---

## 6. Blue/Green 포트 전환 방법

배포 시 `.inc` 파일의 포트만 변경하고 nginx reload하면 무중단 전환이 완료됩니다.

| 서비스 | Blue 포트 | Green 포트 |
|---|---|---|
| core-service | 8080 | 8081 |
| supply-service | 8082 | 8083 |

```bash
# core를 green(8081)으로 전환
echo 'set $core_url http://127.0.0.1:8081;' | sudo tee /etc/nginx/bluegreen/service-url-core.inc
sudo nginx -t && sudo systemctl reload nginx

# supply를 green(8083)으로 전환
echo 'set $supply_url http://127.0.0.1:8083;' | sudo tee /etc/nginx/bluegreen/service-url-supply.inc
sudo nginx -t && sudo systemctl reload nginx
```

---

## 7. 트러블슈팅

| 에러 메시지 | 원인 | 해결 방법 |
|---|---|---|
| `"set" directive is not allowed here` | include가 server{} 블록 밖에 위치 | include를 server{} 블록 안으로 이동 |
| `invalid variable name "\$core_url"` | .inc 파일에 백슬래시(`\$`) 포함 | 홑따옴표로 파일 재작성 |
| `systemclt: command not found` | 오타 (systemclt) | `systemctl`로 수정 |
| `502 Bad Gateway` | 앱이 해당 포트에서 미실행 | 앱 기동 후 재시도 |


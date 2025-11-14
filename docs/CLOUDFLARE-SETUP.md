# Cloudflare DNS-01 챌린지 설정 가이드

Auto-Cert에서 Cloudflare를 DNS 프로바이더로 사용하여 Let's Encrypt 인증서를 자동 발급받는 방법을 설명합니다.

## 목차
- [개요](#개요)
- [Cloudflare API 토큰 생성](#cloudflare-api-토큰-생성)
- [환경 변수 설정](#환경-변수-설정)
- [설정 검증](#설정-검증)
- [문제 해결](#문제-해결)

## 개요

Cloudflare DNS-01 챌린지는 DNS TXT 레코드를 자동으로 생성/삭제하여 도메인 소유권을 증명하는 방식입니다.

**장점:**
- ✅ 와일드카드 인증서 지원 (`*.example.com`)
- ✅ 내부 서버도 가능 (외부 HTTP 접근 불필요)
- ✅ 완전 자동화 (수동 개입 없음)
- ✅ 다중 도메인/서브도메인 지원

**필요사항:**
- Cloudflare에 도메인이 등록되어 있어야 함
- Cloudflare API 토큰 또는 Global API Key 필요

## Cloudflare API 토큰 생성

### 방법 1: API Token (권장)

API Token은 특정 권한만 부여할 수 있어 보안상 안전합니다.

1. **Cloudflare 대시보드 접속**
   - https://dash.cloudflare.com/profile/api-tokens

2. **Create Token 클릭**

3. **Edit zone DNS 템플릿 선택** 또는 **Create Custom Token**

4. **권한 설정**
   ```
   Zone - DNS - Edit
   Zone - Zone - Read
   ```

5. **Zone Resources 설정**
   - Include - Specific zone - `example.com` (또는 All zones)

6. **Continue to summary → Create Token**

7. **토큰 복사** (한 번만 표시됩니다!)
   ```
   예: Yjs3bVhGT2xQZ1VpdVBhNkJ...
   ```

### 방법 2: Global API Key (레거시)

보안상 API Token 사용을 권장하지만, 기존 시스템과의 호환성을 위해 Global API Key도 지원합니다.

1. **Cloudflare 대시보드 접속**
   - https://dash.cloudflare.com/profile/api-tokens

2. **Global API Key → View**

3. **이메일과 API Key 모두 필요**

## 환경 변수 설정

### API Token 방식 (권장)

`.env` 파일 또는 시스템 환경 변수에 추가:

```bash
# ACME DNS-01 프로바이더 설정
ACME_DNS01_PROVIDER=cloudflare

# Cloudflare API Token
CLOUDFLARE_API_TOKEN=your_api_token_here
```

또는 `application.yml`에 직접 설정 (비권장 - 보안상 환경변수 사용 권장):

```yaml
cloudflare:
  api-token: your_api_token_here
```

### Email + API Key 방식

```bash
# ACME DNS-01 프로바이더 설정
ACME_DNS01_PROVIDER=cloudflare

# Cloudflare 계정 정보
CLOUDFLARE_EMAIL=your-email@example.com
CLOUDFLARE_API_KEY=your_global_api_key_here
```

또는 `application.yml`에 직접 설정:

```yaml
cloudflare:
  email: your-email@example.com
  api-key: your_global_api_key_here
```

### 기타 ACME 설정

```bash
# ACME 서버 (Production)
ACME_DIRECTORY_URL=https://acme-v02.api.letsencrypt.org/directory

# ACME 계정 이메일
ACME_ACCOUNT_EMAIL=admin@example.com

# 기본 챌린지 타입
ACME_DEFAULT_CHALLENGE_TYPE=DNS_01

# DNS 전파 대기 타임아웃 (초)
ACME_DNS_PROPAGATION_TIMEOUT=300
```

## application.yml 설정

`api/src/main/resources/application.yml`에서 기본값 확인:

```yaml
autocert:
  certificate:
    acme:
      # DNS-01 챌린지 프로바이더 (manual, cloudflare, route53)
      dns01-provider: ${ACME_DNS01_PROVIDER:cloudflare}
      
      # DNS 전파 대기 타임아웃 (초)
      dns-propagation-timeout: ${ACME_DNS_PROPAGATION_TIMEOUT:300}

# Cloudflare DNS Provider (DNS-01 챌린지용)
cloudflare:
  # API Token 방식 (권장) - 환경변수로 설정
  api-token: ${CLOUDFLARE_API_TOKEN:#{null}}
  
  # Email + API Key 방식 (레거시) - 환경변수로 설정
  email: ${CLOUDFLARE_EMAIL:#{null}}
  api-key: ${CLOUDFLARE_API_KEY:#{null}}
```

**참고:** 
- `#{null}` 문법은 환경변수가 없을 때 Java `null`로 처리
- 보안상 환경변수(`.env` 또는 시스템 환경변수) 사용 권장
- `application.yml`에 직접 입력하면 Git에 커밋될 위험이 있음

## 설정 검증

### 1. 애플리케이션 시작

```bash
./gradlew :api:bootRun
```

### 2. 로그 확인

성공적으로 설정되면 다음과 같은 로그가 출력됩니다:

```
INFO  c.h.a.c.a.d.CloudflareDnsProvider - Cloudflare DNS provider initialized with API Token
INFO  c.h.a.c.a.d.DnsProviderFactory      - Loading DNS provider: cloudflare
INFO  c.h.a.c.a.d.DnsProviderFactory      - DNS provider loaded successfully: cloudflare (CloudflareDnsProvider)
```

### 3. 인증서 발급 테스트

테스트 도메인으로 인증서 발급을 시도합니다:

```bash
curl -X POST http://localhost:8080/api/certificates/issue \
  -H "Content-Type: application/json" \
  -d '{
    "domains": ["test.example.com"],
    "challengeType": "DNS_01"
  }'
```

로그에서 DNS 레코드 추가/삭제 과정을 확인할 수 있습니다:

```
INFO  c.h.a.c.a.d.CloudflareDnsProvider - Adding DNS TXT record to Cloudflare: _acme-challenge.test.example.com = xxx
INFO  c.h.a.c.a.d.CloudflareDnsProvider - DNS TXT record added successfully to Cloudflare
INFO  c.h.a.c.a.d.CloudflareDnsProvider - Waiting for DNS propagation... (max 300 seconds)
INFO  c.h.a.c.a.d.CloudflareDnsProvider - DNS propagation verified after 3 attempts
```

## 문제 해결

### 에러: "Cloudflare credentials not configured"

**원인:** API 토큰 또는 Email+API Key가 설정되지 않음

**해결:**
```bash
# .env 파일 확인
cat .env | grep CLOUDFLARE

# 환경변수 확인
echo $CLOUDFLARE_API_TOKEN
```

### 에러: "Zone not found for domain"

**원인:** 해당 도메인이 Cloudflare에 등록되어 있지 않음

**해결:**
1. Cloudflare 대시보드에서 도메인 확인
2. 도메인의 네임서버가 Cloudflare로 변경되었는지 확인
3. API 토큰의 Zone Resources 권한 확인

### 에러: "Authentication error" (401/403)

**원인:** API 토큰 또는 API Key가 잘못됨

**해결:**
1. API 토큰 재확인 (복사 시 공백 포함 여부)
2. API 토큰 권한 확인 (Zone.DNS Edit 필요)
3. Global API Key 사용 시 이메일 정확성 확인

### 에러: "DNS propagation timeout"

**원인:** DNS 레코드가 전파되는 데 시간이 오래 걸림

**해결:**
1. `ACME_DNS_PROPAGATION_TIMEOUT` 값 증가 (예: 600초)
   ```bash
   ACME_DNS_PROPAGATION_TIMEOUT=600
   ```
2. Cloudflare DNS 서버 상태 확인
3. 수동으로 DNS 레코드 확인:
   ```bash
   # Google Public DNS로 확인
   dig @8.8.8.8 _acme-challenge.example.com TXT
   
   # Cloudflare DNS로 확인
   dig @1.1.1.1 _acme-challenge.example.com TXT
   
   # nslookup으로 확인
   nslookup -type=TXT _acme-challenge.example.com 8.8.8.8
   ```

**참고:**
- 시스템은 자동으로 20초 대기 후 DNS 전파 확인 시작
- Google Public DNS (8.8.8.8)로 실제 전파 여부 검증
- 10초 간격으로 재시도

### 에러: "Failed to add DNS record: 81057"

**원인:** 동일한 TXT 레코드가 이미 존재

**해결:**
1. Cloudflare 대시보드에서 기존 `_acme-challenge` 레코드 삭제
2. 애플리케이션 재시작하여 정리(cleanup) 로직 실행

## 와일드카드 인증서 발급

Cloudflare DNS-01을 사용하면 와일드카드 인증서를 쉽게 발급받을 수 있습니다:

```bash
curl -X POST http://localhost:8080/api/certificates/issue \
  -H "Content-Type: application/json" \
  -d '{
    "domains": ["*.example.com", "example.com"],
    "challengeType": "DNS_01"
  }'
```

**주의사항:**
- 와일드카드(`*.example.com`)와 루트 도메인(`example.com`)은 별도로 지정해야 함
- 와일드카드는 서브도메인만 커버 (예: `test.example.com`)
- 중첩 서브도메인은 커버하지 않음 (예: `api.test.example.com` 불가)

## 보안 권장사항

1. **API Token 사용 권장**
   - Global API Key보다 세밀한 권한 제어 가능
   - 만료 기간 설정 가능
   - IP 제한 가능

2. **환경 변수 보호**
   - `.env` 파일을 `.gitignore`에 추가
   - 프로덕션에서는 HashiCorp Vault 등 Secret 관리 도구 사용

3. **API 토큰 정기 갱신**
   - 3-6개월마다 토큰 재발급 권장

4. **최소 권한 원칙**
   - Zone.DNS Edit 권한만 부여
   - 특정 Zone에만 접근 허용

## 참고 자료

- [Cloudflare API Documentation](https://developers.cloudflare.com/api/)
- [ACME Protocol (RFC 8555)](https://tools.ietf.org/html/rfc8555)
- [Let's Encrypt DNS-01 Challenge](https://letsencrypt.org/docs/challenge-types/#dns-01-challenge)

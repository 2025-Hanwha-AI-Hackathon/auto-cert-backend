# Auto-Cert 테스트 가이드

이 문서는 Auto-Cert 애플리케이션의 ACME 인증서 발급 기능을 테스트하는 방법을 설명합니다.

## 사전 준비

### 1. 데이터베이스 설정

PostgreSQL 데이터베이스가 실행 중이어야 합니다.

```bash
# Docker Compose로 PostgreSQL 시작
docker-compose up -d postgres

# 또는 로컬 PostgreSQL 사용
# 데이터베이스 생성
createdb autocert
```

### 2. 환경 변수 설정

`.env` 파일을 생성하고 필요한 환경 변수를 설정합니다.

```bash
# .env 파일 편집
# 최소한 다음 항목을 설정:
# - ACME_ACCOUNT_EMAIL: 유효한 이메일 주소
# - DB_URL, DB_USERNAME, DB_PASSWORD: 데이터베이스 정보
```

### 3. 데이터베이스 마이그레이션

```bash
# Flyway 마이그레이션 실행
./gradlew :domain:flywayMigrate

# 마이그레이션 상태 확인
./gradlew :domain:flywayInfo
```

## 테스트 시나리오

### Scenario 1: 애플리케이션 시작 테스트

```bash
# 애플리케이션 빌드
./gradlew clean build

# 애플리케이션 실행
./gradlew :api:bootRun

# 또는 JAR 파일 실행
java -jar api/build/libs/api-1.0.0-SNAPSHOT.jar
```

**확인 사항:**
- 애플리케이션이 정상적으로 시작되는지 확인
- 로그에서 "Started AutoCertApplication" 메시지 확인
- http://localhost:8080/actuator/health 접속 확인

### Scenario 2: Swagger UI 확인

```bash
# 브라우저에서 접속
open http://localhost:8080/swagger-ui.html
```

**확인 사항:**
- Certificate API 엔드포인트 확인
- ACME Account API 확인 (있는 경우)

### Scenario 3: ACME 계정 생성 테스트 (자동)

애플리케이션이 시작되면 자동으로 ACME 계정이 생성됩니다.

```bash
# 로그에서 확인
# "Creating new ACME account for email: ..."
# "ACME account created successfully"
```

**데이터베이스 확인:**

```sql
-- PostgreSQL에 접속
psql -U autocert -d autocert

-- ACME 계정 조회
SELECT id, email, acme_server_url, status, created_at 
FROM acme_accounts;
```

### Scenario 4: HTTP-01 챌린지 테스트 (시뮬레이션)

**주의**: 실제 도메인 소유권이 필요하므로 완전한 테스트는 어렵습니다.

#### 옵션 A: 단위 테스트로 확인

```bash
# 챌린지 핸들러 테스트 실행
./gradlew :certificate-manager:test --tests ChallengeHandlerFactoryTest
./gradlew :certificate-manager:test --tests AcmeAccountServiceTest
```

#### 옵션 B: 로컬 환경 시뮬레이션 (고급)

1. 로컬 DNS 설정 또는 ngrok 사용
2. 웹서버 디렉토리 설정 (`ACME_HTTP01_WEBROOT`)
3. API 호출로 인증서 발급 시도

### Scenario 5: API 엔드포인트 테스트

#### 5-1. 모든 인증서 조회

```bash
curl -X GET "http://localhost:8080/api/certificates?page=0&size=10" \
  -H "accept: application/json"
```

#### 5-2. 인증서 생성 (Staging 환경)

**주의**: 실제 도메인이 필요하며, HTTP-01 챌린지를 위해서는:
- 도메인이 현재 서버를 가리켜야 함
- 포트 80이 열려있어야 함
- `/var/www/html/.well-known/acme-challenge/` 디렉토리 접근 가능

```bash
# 테스트용 (실패할 것으로 예상)
curl -X POST "http://localhost:8080/api/certificates" \
  -H "Content-Type: application/json" \
  -d '{
    "domain": "test.example.com",
    "certificateType": "standard",
    "challengeType": "http-01"
  }'
```

#### 5-3. 인증서 상태 확인

```bash
curl -X GET "http://localhost:8080/api/certificates/1" \
  -H "accept: application/json"
```

## 테스트 결과 확인

### 1. 로그 확인

```bash
# 애플리케이션 로그 확인
tail -f logs/application.log

# 주요 로그 메시지:
# - "ACME account created successfully"
# - "Preparing HTTP-01 challenge for domain"
# - "HTTP-01 challenge validated successfully"
# - "Certificate issued successfully for domain"
```

### 2. 데이터베이스 확인

```sql
-- 인증서 목록 조회
SELECT id, domain, status, issued_at, expires_at, created_at
FROM certificates
ORDER BY created_at DESC;

-- 인증서 상태별 조회
SELECT status, COUNT(*) 
FROM certificates 
GROUP BY status;

-- ACME 계정 확인
SELECT id, email, status, last_used_at 
FROM acme_accounts;
```

### 3. 암호화 확인

```sql
-- 개인키가 암호화되어 있는지 확인
SELECT 
    id, 
    domain,
    LENGTH(private_key_pem) as key_length,
    SUBSTRING(private_key_pem, 1, 20) as key_preview
FROM certificates
WHERE private_key_pem IS NOT NULL;

-- PEM 형식이면 "-----BEGIN"으로 시작
-- Base64 인코딩되어 있으면 암호화된 상태
```

## 트러블슈팅

### 문제 1: "이미 존재하는 도메인입니다" 에러

```sql
-- 기존 인증서 삭제
DELETE FROM certificates WHERE domain = 'test.example.com';
```

### 문제 2: "챌린지 검증 실패"

**원인:**
- 도메인이 현재 서버를 가리키지 않음
- 방화벽에서 포트 80 차단
- 웹서버 디렉토리 권한 문제

**해결:**
- ACME Staging 환경 사용 중인지 확인
- 로그에서 상세 에러 메시지 확인
- DNS 설정 확인: `dig test.example.com`

### 문제 3: "ACME 계정 생성 실패"

**원인:**
- 네트워크 연결 문제
- 잘못된 이메일 형식
- Let's Encrypt API 응답 지연

**해결:**
- 이메일 주소 확인
- 네트워크 연결 확인
- Staging URL 확인: `https://acme-staging-v02.api.letsencrypt.org/directory`

### 문제 4: "데이터베이스 연결 실패"

```bash
# PostgreSQL 상태 확인
docker-compose ps postgres

# PostgreSQL 로그 확인
docker-compose logs postgres

# 데이터베이스 연결 테스트
psql -U autocert -d autocert -h localhost -p 5432
```

## 권장 테스트 순서

1. ✅ **단위 테스트 실행**: `./gradlew test`
2. ✅ **데이터베이스 마이그레이션**: `./gradlew :domain:flywayMigrate`
3. ✅ **애플리케이션 시작**: `./gradlew :api:bootRun`
4. ✅ **Swagger UI 확인**: http://localhost:8080/swagger-ui.html
5. ✅ **ACME 계정 자동 생성 확인**: 로그 및 DB 조회
6. ⚠️ **인증서 발급 테스트**: 실제 도메인 필요 (선택)

## 다음 단계

현재 구현된 기능:
- ✅ ACME 계정 관리
- ✅ HTTP-01 챌린지 핸들러
- ✅ DNS-01 챌린지 핸들러 (Manual)
- ✅ 인증서 발급 및 저장
- ✅ 인증서 갱신
- ✅ 개인키 암호화

향후 구현 예정:
- ⏳ 자동 갱신 스케줄러
- ⏳ 인증서 배포 서비스
- ⏳ 웹서버 통합
- ⏳ 모니터링 및 알림

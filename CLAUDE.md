# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소의 코드를 다룰 때 참고하는 가이드입니다.

## 프로젝트 개요

Auto-Cert는 Spring Boot 3.x와 Java 21로 구축된 SSL/TLS 인증서 관리 자동화 솔루션입니다. ACME 프로토콜을 통한 갱신, 대상 서버로의 배포, 웹서버 설정, 무중단 재시작, 모니터링 등 인증서의 전체 생명주기를 자동화합니다.

**핵심 기술 스택:**
- Java 21 (LTS)
- Spring Boot 3.5.7
- Gradle 8.14.3 (Groovy DSL)
- PostgreSQL 16
- ACME4J 3.3.1 (Let's Encrypt 연동)

## 빌드 및 개발 명령어

### 빌드
```bash
# 전체 빌드 (모든 모듈)
./gradlew clean build

# 특정 모듈 빌드
./gradlew :certificate-manager:build
./gradlew :api:build

# 테스트 없이 빌드
./gradlew build -x test
```

### 테스트 실행
```bash
# 전체 테스트 실행
./gradlew test

# 특정 모듈 테스트 실행
./gradlew :domain:test
./gradlew :certificate-manager:test

# 단일 테스트 클래스 실행
./gradlew :api:test --tests CertificateRenewalServiceTest

# 단일 테스트 메서드 실행
./gradlew :api:test --tests CertificateRenewalServiceTest.shouldRenewCertificateWhenExpiring

# 코드 커버리지와 함께 실행
./gradlew test jacocoTestReport
```

### 애플리케이션 실행
```bash
# Gradle로 실행
./gradlew :api:bootRun

# 특정 프로파일로 실행
./gradlew :api:bootRun --args='--spring.profiles.active=dev'
./gradlew :api:bootRun --args='--spring.profiles.active=prod'

# JAR 직접 실행
java -jar api/build/libs/api-1.0.0-SNAPSHOT.jar

# 디버그 모드 (포트 5005)
./gradlew :api:bootRun --debug-jvm
```

### Docker
```bash
# Docker Compose로 빌드 및 실행 (PostgreSQL, Redis, Vault 포함)
docker-compose up -d

# Docker 이미지만 빌드
docker build -t auto-cert:latest -f api/Dockerfile .

# 모든 컨테이너 중지
docker-compose down
```

### 코드 품질 검사
```bash
# Checkstyle 실행
./gradlew checkstyleMain checkstyleTest

# 의존성 취약점 검사
./gradlew dependencyCheckAnalyze
```

## 아키텍처

레이어드 아키텍처를 사용하는 **멀티 모듈 모노레포** 구조입니다:

### 모듈 의존성 계층 구조
```
api (Application Layer - 애플리케이션 계층)
 ├─> certificate-manager (인증서 관리 및 배포)
 ├─> webserver-integration
 ├─> reload-service
 ├─> monitoring-service
 └─> domain (Core Layer - 핵심 계층)
      └─> common (Foundation Layer - 기반 계층)
```

**의존성 규칙:** 모듈은 같은 계층 또는 하위 계층의 모듈만 의존할 수 있습니다. 순환 의존성을 절대 만들지 마세요.

### 핵심 모듈 설명

**common/** - 모든 모듈이 공유하는 기반 유틸리티
- 예외 처리, 상수, 유틸리티 클래스
- Spring 의존성 없음, 순수 Java 유틸리티만 포함

**domain/** - 도메인 모델 및 비즈니스 로직
- JPA 엔티티: Certificate, Server, Deployment, RenewalHistory, ServerConfig
- Repository 인터페이스 (Spring Data JPA)
- 도메인 서비스 및 이벤트
- Flyway 마이그레이션 (`src/main/resources/db/migration/`)

**certificate-manager/** - 인증서 생명주기 관리 및 배포
- ACME 프로토콜 통합 (Let's Encrypt, ZeroSSL)
- DNS-01, HTTP-01 챌린지 핸들러
- 인증서 갱신 스케줄러 (cron: "0 0 2 * * ?" - 매일 새벽 2시)
- 인증서 타입별 갱신 전략 패턴
- 인증서 검증 및 저장
- SSH/SFTP를 통한 대상 서버 인증서 배포 (sshj 라이브러리)
- 배포 재시도 로직 (최대 3회, exponential backoff)
- 다중 서버 병렬 배포 지원

**webserver-integration/** - 웹서버 설정 통합
- 어댑터 패턴으로 다양한 서버 지원: Nginx, Apache, Tomcat, IIS
- 템플릿 기반 설정 생성 (Thymeleaf)
- 설정 검증 (nginx -t, apachectl configtest)
- 변경 전 자동 백업

**reload-service/** - 서버 재시작 및 헬스체크
- Graceful reload 실행 (무중단)
- 재시작 후 HTTP 헬스체크
- 헬스체크 실패 시 자동 롤백
- 서버 타입별 커스텀 재시작 스크립트 지원

**monitoring-service/** - 모니터링 및 알림
- 인증서 만료 추적 (30일 전 경고)
- Email, Slack, Webhook 알림
- Prometheus 메트릭 내보내기
- 감사 추적을 위한 이벤트 로깅

**api/** - 메인 애플리케이션 (API Gateway)
- 모든 서비스의 REST 컨트롤러
- JWT 인증을 사용하는 Spring Security
- `/swagger-ui.html`에서 제공되는 Swagger/OpenAPI 문서
- 스케줄러 설정
- `@SpringBootApplication` 어노테이션이 있는 유일한 모듈

## 주요 디자인 패턴

**어댑터 패턴 (Adapter Pattern):** `webserver-integration`에서 여러 웹서버 타입(Nginx, Apache, Tomcat, IIS)을 지원하기 위해 사용됩니다. 각 서버는 공통 인터페이스를 구현하는 자체 어댑터를 가집니다.

**전략 패턴 (Strategy Pattern):** `certificate-manager/renewal/strategy`에서 다양한 갱신 전략(표준, 와일드카드, 멀티도메인)을 위해 사용됩니다.

**레포지토리 패턴 (Repository Pattern):** `domain` 모듈의 Spring Data JPA 레포지토리가 데이터 접근 추상화를 제공합니다.

**이벤트 주도 (Event-Driven):** `domain/event`의 도메인 이벤트를 통해 모듈 간 느슨한 결합 통신을 구현합니다 (인증서 갱신됨, 배포 완료 등).

## 데이터베이스 스키마

주요 테이블 (Flyway 스크립트는 `domain/src/main/resources/db/migration/` 참조):

- **CERTIFICATES**: 인증서 상세 정보 저장 (domain, issued_at, expires_at, certificate_pem, private_key_pem)
- **SERVERS**: 대상 서버 정보 (hostname, ip_address, server_type, port, status)
- **DEPLOYMENTS**: 배포 이력 (certificate_id, server_id, deployed_at, status)
- **RENEWAL_HISTORY**: 인증서 갱신 감사 로그
- **SERVER_CONFIGS**: 서버별 설정 스냅샷

## 설정

**환경 프로파일:**
- `dev` - 개발 환경 (localhost의 PostgreSQL, 디버그 로깅)
- `prod` - 운영 환경 (외부 데이터베이스, info 로깅)

**주요 설정 속성:**
```yaml
autocert:
  certificate:
    acme:
      directory-url: https://acme-v02.api.letsencrypt.org/directory
      account-email: ${ACME_EMAIL}
    renewal:
      days-before-expiry: 30
      cron: "0 0 2 * * ?"
  distribution:
    ssh:
      timeout: 30000
      max-retries: 3
  monitoring:
    alert:
      email.enabled: ${ALERT_EMAIL_ENABLED:false}
      slack.enabled: ${ALERT_SLACK_ENABLED:false}
```

## 보안 고려사항

**인증서 저장:** 프라이빗 키는 데이터베이스에 AES-256으로 암호화되어 저장되며, 배포 시에만 메모리 내에서 복호화됩니다.

**자격증명 관리:** 모든 SSH 자격증명과 API 키는 HashiCorp Vault에 저장되며, 애플리케이션 설정이나 데이터베이스에는 절대 저장되지 않습니다.

**API 보안:** 헬스체크를 제외한 모든 엔드포인트는 JWT 기반 인증이 필요합니다.

## 코드 작업 가이드

**새로운 웹서버 지원 추가:**
1. `webserver-integration/src/main/java/com/autocert/webserver/adapter/{servername}/`에 어댑터 생성
2. `ServerAdapter` 인터페이스 구현: `generateConfig()`, `validateConfig()`, `getReloadCommand()` 메서드
3. `webserver-integration/src/main/resources/templates/{servername}/`에 서버별 설정 템플릿 추가
4. `WebServerAdapterRegistry`에 어댑터 등록

**새로운 ACME 프로바이더 추가:**
1. `certificate-manager/src/main/java/com/autocert/certificate/acme/provider/`에 프로바이더 클래스 생성
2. `AcmeProvider` 베이스 클래스 확장
3. 프로바이더별 인증 및 챌린지 처리 구현
4. `AcmeProviderFactory`에 프로바이더 추가

**Flyway 데이터베이스 마이그레이션:**
- SQL 파일을 `domain/src/main/resources/db/migration/`에 배치
- 네이밍: `V{version}__{description}.sql` (예: `V001__create_certificates_table.sql`)
- 기존 마이그레이션은 절대 수정하지 말고, 항상 새 버전 생성

## 문서

**아키텍처 다이어그램:** `docs/*.mmd` - GitHub 또는 Mermaid 미리보기 도구로 볼 수 있는 Mermaid 다이어그램

**상세 모듈 구조:** `docs/PROJECT-STRUCTURE.md` - 모듈 의존성, 빌드 설정, 패키지 구조에 대한 완전한 가이드

**API 문서:** 애플리케이션 실행 후 `http://localhost:8080/swagger-ui.html`에서 인터랙티브 API 문서 확인

## 현재 개발 단계

**Phase 1 (MVP)** - 아키텍처 설계 완료. 다음 단계:
1. ACME 프로토콜 통합 구현
2. 기본 인증서 갱신 기능
3. Nginx 서버 지원
4. 간단한 모니터링 대시보드

전체 개발 단계는 README.md의 로드맵을 참조하세요.

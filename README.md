# Auto-Cert 설계 문서

SSL/TLS 인증서 관리 자동화 솔루션의 설계 문서입니다.

## 📑 문서 목록

### 아키텍처 다이어그램

#### 1. [시스템 아키텍처](mermaid/01-system-architecture.mmd)

전체 시스템의 구조와 컴포넌트 간 관계

- Control Plane (API Gateway, Dashboard, Scheduler, Database)
- Core Services (인증서 관리, 배포, 적용, 재기동, 모니터링)
- External Services (ACME Server, DNS Provider)
- Target Servers (Nginx, Apache, Tomcat)

#### 2. [인증서 갱신 프로세스](mermaid/02-certificate-renewal-process.mmd)

ACME 프로토콜 기반 자동 갱신 흐름

- 만료 예정 인증서 확인
- ACME 챌린지 (DNS-01, HTTP-01)
- 인증서 발급 및 저장
- 알림 전송

#### 3. [배포 및 적용 프로세스](mermaid/03-deployment-application-process.mmd)

서버 배포 및 웹서버 적용 워크플로우

- 인증서 검증
- 다중 서버 배포
- 웹서버 설정 업데이트 (Nginx/Apache/Tomcat)
- Graceful reload 및 헬스체크
- 롤백 처리

#### 4. [컴포넌트 상세 설계](mermaid/04-component-design.mmd)

각 서비스 모듈의 내부 구조

- Certificate Manager (ACME Client, Renewal Scheduler)
- Web Server Integration (Config Generator, Server Adapter)
- Monitoring Dashboard (Expiry Tracker, Alert Manager)

#### 5. [모니터링 대시보드](mermaid/05-monitoring-dashboard.mmd)

대시보드 UI 구조

- 뷰: Overview, Certificate List, Timeline, Logs
- 메트릭: 전체 인증서, 만료 임박, 갱신 이력
- 액션: 수동 갱신, 강제 배포, 설정 테스트

#### 6. [데이터베이스 스키마](mermaid/06-database-schema.mmd)

ERD 및 테이블 정의

- CERTIFICATES (인증서 정보)
- SERVERS (서버 정보)
- DEPLOYMENTS (배포 이력)
- RENEWAL_HISTORY (갱신 이력)
- SERVER_CONFIGS (서버 설정)

#### 7. [모듈 의존성 그래프](mermaid/07-module-dependency-graph.mmd)

Spring Boot 멀티 모듈 의존성 관계

- Application Layer: api
- Service Layer: certificate-manager
- Core Layer: domain
- Foundation Layer: common

## 📋 주요 기능

### 1. 인증서 갱신 (Certificate Renewal)

- ACME 프로토콜 지원 (Let's Encrypt, ZeroSSL)
- 자동 갱신 스케줄러 (만료 30일 전)
- DNS-01, HTTP-01 챌린지
- Wildcard 인증서 지원

### 2. 서버 배포 (Distribution)

- SSH/SCP 기반 안전한 전송
- 다중 서버 동시 배포
- 재시도 로직 (최대 3회)
- 암호화된 자격증명 관리

### 3. 웹서버 적용 (Web Server Integration)

- Nginx, Apache, Tomcat, IIS 지원
- 자동 설정 생성
- 설정 검증
- 백업 및 롤백

### 4. 재기동 (Reload)

- Graceful reload (무중단)
- 헬스체크 자동화
- 실패 시 자동 롤백
- 커스텀 스크립트 지원

### 5. 모니터링 (Monitoring)

- 실시간 만료 추적
- 갱신 이력 및 통계
- 알림 (Email, Slack, Webhook)
- 감사 로그

## 🚀 빠른 시작

### 환경 설정

`.env` 파일을 생성하여 필요한 환경 변수를 설정합니다:

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/autocert
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Certificate Encryption (필수!)
# AES-256 암호화 키 - 인증서 개인키 암호화에 사용
# 주의: 이 키는 한 번 설정하면 절대 변경하지 마세요!
# 키 생성: openssl rand -base64 32
CERTIFICATE_ENCRYPTION_KEY=your_base64_encryption_key_here

# ACME (Staging - 테스트용)
ACME_DIRECTORY_URL=https://acme-staging-v02.api.letsencrypt.org/directory
ACME_ACCOUNT_EMAIL=your-email@example.com
ACME_KEY_ALGORITHM=RSA
ACME_KEY_SIZE=2048
ACME_ACCEPT_TERMS=true
ACME_DEFAULT_CHALLENGE_TYPE=DNS_01

# Cloudflare (DNS-01 챌린지용)
CLOUDFLARE_API_TOKEN=your_cloudflare_api_token
```

💡 **Tip**: Production 환경으로 전환하려면 `ACME_DIRECTORY_URL`을 `https://acme-v02.api.letsencrypt.org/directory`로 변경하세요.

### 프로젝트 빌드

```bash
# 전체 빌드
./gradle clean build

# 특정 모듈 빌드
./gradle :certificate-manager:build

# 테스트 실행
./gradlew test

# 테스트 없이 빌드
./gradlew build -x test
```

### 애플리케이션 실행

#### 1. Docker Compose로 실행

```bash
# PostgreSQL + Nginx 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 중지
docker-compose stop

# 완전 삭제
docker-compose down -v
```

#### 2. Spring Boot 애플리케이션 실행

```bash
# Gradle로 실행
./gradle :api:bootRun

# 특정 프로파일로 실행
./gradle :api:bootRun --args='--spring.profiles.active=dev'

# JAR 직접 실행
java -jar api/build/libs/api-1.0.0-SNAPSHOT.jar

# 디버그 모드 (포트 5005)
./gradle :api:bootRun --debug-jvm
```

## 📂 프로젝트 구조

```
auto-cert/
├── certs/                                 # 인증서 디렉토리
│   ├── localhost.crt                      # Self-signed
│   ├── localhost.key
│   └── .gitkeep                           # Git 추적용
├── mermaid/                               # 아키텍처 다이어그램
├── docs/                                  # 설계 문서
├── common/                                # 공통 유틸리티
├── domain/                                # 도메인 모델
├── monitoring-service/                    # 모니터링
├── api/                                   # API Gateway (Main)
└── docker-compose.yml                     # Docker 설정
```

상세한 구조는 [PROJECT-STRUCTURE.md](docs/PROJECT_STRUCTURE.md)를 참조하세요.

## 📚 참고 자료

### 프로젝트 문서

- **[CLOUDFLARE-SETUP.md](docs/CLOUDFLARE-SETUP.md)** - Cloudflare DNS 설정
- **[CLAUDE.md](CLAUDE.md)** - Claude Code용 프로젝트 가이드

### 기술 문서

- [ACME Protocol (RFC 8555)](https://tools.ietf.org/html/rfc8555)
- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Spring Boot 3.x Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [acme4j Documentation](https://shredzone.org/maven/acme4j/)

## 🤝 기여 가이드

1. 브랜치 생성: `git checkout -b feature/new-feature`
2. 변경사항 커밋: `git commit -am 'Add new feature'`
3. 브랜치 푸시: `git push origin feature/new-feature`
4. Pull Request 생성

## 📄 라이선스

이 프로젝트는 내부용으로 작성되었습니다.

---

**문서 버전**: 1.1.0
**최종 수정일**: 2025-11-20
**작성자**: Auto-Cert Development Team

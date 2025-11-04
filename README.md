# Auto-Cert 설계 문서

SSL/TLS 인증서 관리 자동화 솔루션의 설계 문서입니다.

## 📑 문서 목록

### 프로젝트 구조
- **[PROJECT-STRUCTURE.md](PROJECT-STRUCTURE.md)** - Spring Boot 3.x + JDK 21 모노레포 구조 상세 가이드

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
- Distribution Service (SSH Manager, Credential Vault)
- Web Server Integration (Config Generator, Server Adapter)
- Reload Service (Health Checker, Rollback Manager)
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
- Service Layer: certificate-manager, distribution-service, etc.
- Core Layer: domain
- Foundation Layer: common

## 🏗️ 기술 스택

### Backend
- **Language**: JAVA 21
- **Framework**: Spring Boot 3.5.7
- **JDK**: 21 (LTS)
- **Build Tool**: Gradle 8.14.3
- **Database**: PostgreSQL 16
- **ACME Library**: acme4j 3.3.1

### Infrastructure
- **Container**: Docker
- **CI/CD**: GitHub Actions

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

### 프로젝트 빌드

```bash
# 전체 빌드
./gradlew clean build

# 특정 모듈 빌드
./gradlew :certificate-manager:build

# 테스트 실행
./gradlew test
```

### 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew :api:bootRun

# JAR 실행
java -jar api/build/libs/api-1.0.0-SNAPSHOT.jar

# 프로필 지정
./gradlew :api:bootRun --args='--spring.profiles.active=dev'
```

### Docker로 실행

```bash
# Docker Compose
docker-compose up -d

# 개별 빌드
docker build -t auto-cert:latest -f api/Dockerfile .
docker run -p 8080:8080 auto-cert:latest
```

## 📂 프로젝트 구조

```
auto-cert/
├── mermaid/                        # 아키텍처 다이어그램
├── docs/                           # 설계 문서
├── common/                         # 공통 유틸리티
├── domain/                         # 도메인 모델
├── certificate-manager/            # 인증서 관리
├── distribution-service/           # 배포 서비스
├── webserver-integration/          # 웹서버 통합
├── reload-service/                 # 재시작 서비스
├── monitoring-service/             # 모니터링
└── api/                            # API Gateway (Main)
```

상세한 구조는 [PROJECT-STRUCTURE.md](docs/01-project-structure.md)를 참조하세요.

## 🔒 보안 고려사항

1. **인증서 및 키 관리**
   - AES-256 암호화 저장
   - 메모리 내 복호화
   - 접근 로그 기록

2. **서버 접근**
   - SSH 키 기반 인증
   - 최소 권한 원칙
   - 접근 감사

3. **API 보안**
   - JWT 인증
   - Rate limiting
   - IP 화이트리스트

4. **네트워크**
   - TLS 1.3
   - mTLS 옵션
   - VPN/Private Network

## 📈 구현 로드맵

### Phase 1: MVP 
- [x] 아키텍처 설계
- [ ] ACME 프로토콜 통합
- [ ] 기본 인증서 갱신
- [ ] Nginx 지원
- [ ] 간단한 대시보드
- [ ] 알림 시스템

### Phase 2: 고급 기능 
- [ ] Apache, Tomcat 지원
- [ ] 다중 서버 배포
- [ ] 자동 롤백
- [ ] 대시보드 고도화
- [ ] 통계 및 리포트
- [ ] API 확장
- [ ] 플러그인 시스템
- [ ] RBAC 권한 관리
- [ ] 멀티 테넌시
- [ ] 고가용성 구성
- [ ] 감사 로그 강화

## 📚 참고 자료

- [ACME Protocol (RFC 8555)](https://tools.ietf.org/html/rfc8555)
- [Let's Encrypt Documentation](https://letsencrypt.org/mermaid/)
- [Spring Boot 3.x Reference](https://mermaid.spring.io/spring-boot/mermaid/current/reference/html/)
- [Kotlin Documentation](https://kotlinlang.org/mermaid/home.html)
- [acme4j Documentation](https://shredzone.org/maven/acme4j/)

## 🤝 기여 가이드

1. 브랜치 생성: `git checkout -b feature/new-feature`
2. 변경사항 커밋: `git commit -am 'Add new feature'`
3. 브랜치 푸시: `git push origin feature/new-feature`
4. Pull Request 생성

## 📄 라이선스

이 프로젝트는 내부용으로 작성되었습니다.

---

**문서 버전**: 1.0.1
**최종 수정일**: 2025-11-04
**작성자**: Auto-Cert Development Team

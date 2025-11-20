# Auto-Cert Monorepo Structure

Spring Boot 3.x + JDK 21 기반 인증서 관리 자동화 솔루션 모노레포 구조

## 프로젝트 개요

- **JDK**: 21 (LTS)
- **Spring Boot**: 3.5.7
- **Build Tool**: Gradle 8.14.3 with Groovy DSL
- **Language**: Java 21
- **Architecture**: Multi-module Monorepo
- **Frontend**: 별도 레포지토리 (React/Vue.js)

## 디렉토리 구조

```
auto-cert/
├── build.gradle                       # Root build configuration
├── settings.gradle                    # Multi-module settings
├── gradle.properties                  # Gradle properties
├── gradlew                           # Gradle wrapper
├── gradlew.bat
├── .gitignore
├── .claudeignore
├── README.md
├── docs/                             # Documentation              
│── mermaid/*.mmd                     # Mermaid diagrams
├── common/                           # 공통 모듈
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/hwgi/autocert/common/
│       │   │   ├── config/          # 공통 설정
│       │   │   ├── exception/       # 예외 처리
│       │   │   ├── util/            # 유틸리티
│       │   │   └── constants/       # 상수
│       │   └── resources/
│       └── test/
│           ├── java/com/ahwgi/utocert/common/
│           └── resources/
│
├── domain/                           # 도메인 모델 및 비즈니스 로직
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/hwgi/autocert/domain/
│       │   │   ├── model/           # Entity, VO
│       │   │   │   ├── certificate/
│       │   │   │   ├── server/
│       │   │   │   ├── deployment/
│       │   │   │   └── audit/
│       │   │   ├── repository/      # Repository interfaces
│       │   │   ├── service/         # Domain services
│       │   │   └── event/           # Domain events
│       │   └── resources/
│       │       └── db/
│       │           └── migration/   # Flyway migrations
│       └── test/
│           ├── java/com/hwgi/autocert/domain/
│           └── resources/
│
├── certificate-manager/              # 인증서 관리 및 배포 모듈
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/hwgi/autocert/certificate/
│       │   │   ├── acme/            # ACME 프로토콜 클라이언트
│       │   │   │   ├── client/
│       │   │   │   ├── challenge/   # DNS-01, HTTP-01
│       │   │   │   └── provider/    # Let's Encrypt, ZeroSSL
│       │   │   ├── renewal/         # 갱신 로직
│       │   │   │   ├── scheduler/
│       │   │   │   └── strategy/
│       │   │   ├── store/           # 인증서 저장소
│       │   │   ├── validation/      # 검증
│       │   │   ├── distribution/    # 인증서 배포
│       │   │   │   ├── config/      # 배포 설정
│       │   │   │   ├── ssh/         # SSH/SFTP 클라이언트
│       │   │   │   └── service/     # 배포 서비스
│       │   │   └── config/
│       │   └── resources/
│       │       └── application.yml
│       └── test/
│           ├── java/com/hwgi/autocert/certificate/
│           └── resources/
│
├── webserver-integration/            # 웹서버 통합 모듈
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/hwgi/autocert/webserver/
│       │   │   ├── adapter/         # 서버 어댑터
│       │   │   │   ├── nginx/
│       │   │   │   ├── apache/
│       │   │   │   ├── tomcat/
│       │   │   │   └── iis/
│       │   │   ├── config/          # 설정 생성
│       │   │   │   └── generator/
│       │   │   ├── validator/       # 설정 검증
│       │   │   ├── backup/          # 백업 관리
│       │   │   └── config/
│       │   └── resources/
│       │       ├── templates/       # 설정 템플릿
│       │       │   ├── nginx/
│       │       │   ├── apache/
│       │       │   └── tomcat/
│       │       └── application-webserver.yml
│       └── test/
│           ├── java/com/hwgi/autocert/webserver/
│           └── resources/
│
├── monitoring-service/               # 모니터링 서비스 모듈
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/hwgi/autocert/monitoring/
│       │   │   ├── tracker/         # 추적
│       │   │   │   └── expiry/      # 만료 추적
│       │   │   ├── alert/           # 알림
│       │   │   │   ├── email/
│       │   │   │   ├── slack/
│       │   │   │   └── webhook/
│       │   │   ├── metrics/         # 메트릭
│       │   │   ├── event/           # 이벤트 로깅
│       │   │   └── config/
│       │   └── resources/
│       │       └── application-monitoring.yml
│       └── test/
│           ├── java/com/hwgi/autocert/monitoring/
│           └── resources/
│
└── api/                              # API Gateway (Main Application)
    ├── build.gradle
    └── src/
        ├── main/
        │   ├── java/com/hwgi/autocert/api/
        │   │   ├── AutoCertApplication.java    # Main class
        │   │   ├── controller/                 # REST Controllers
        │   │   │   ├── certificate/
        │   │   │   ├── server/
        │   │   │   ├── deployment/
        │   │   │   └── monitoring/
        │   │   ├── dto/                        # Request/Response DTOs
        │   │   ├── security/                   # Security config
        │   │   ├── scheduler/                  # Schedulers
        │   │   └── config/                     # Application config
        │   └── resources/
        │       ├── application.yml
        │       ├── application-dev.yml
        │       ├── application-prod.yml
        │       └── logback-spring.xml
        └── test/
            ├── java/com/hwgi/autocert/api/
            └── resources/
```

## 모듈 의존성 구조

```
api
 ├─> certificate-manager (인증서 관리 + 배포)
 ├─> webserver-integration
 ├─> monitoring-service
 └─> domain

certificate-manager
 ├─> domain
 └─> common

webserver-integration
 ├─> domain
 └─> common

monitoring-service
 ├─> domain
 └─> common

domain
 └─> common

common
 └─> (no dependencies)
```

## 빌드 및 실행

### 전체 빌드

```bash
./gradlew clean build
```

### 특정 모듈 빌드

```bash
./gradlew :certificate-manager:build
```

### 실행 (API 모듈)

```bash
./gradlew :api:bootRun
```

또는

```bash
java -jar api/build/libs/api-1.0.0-SNAPSHOT.jar
```

### 프로필별 실행

```bash
# Development
./gradlew :api:bootRun --args='--spring.profiles.active=dev'

# Production
java -jar api/build/libs/api-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

## Docker 지원

### Dockerfile (api/Dockerfile)

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :api:bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: autocert
      POSTGRES_USER: autocert
      POSTGRES_PASSWORD: autocert
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  vault:
    image: hashicorp/vault:latest
    environment:
      VAULT_DEV_ROOT_TOKEN_ID: root
    ports:
      - "8200:8200"
    cap_add:
      - IPC_LOCK

  api:
    build:
      context: .
      dockerfile: api/Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/autocert
      SPRING_DATA_REDIS_HOST: redis
      SPRING_CLOUD_VAULT_HOST: vault
    depends_on:
      - postgres
      - redis
      - vault

volumes:
  postgres-data:
```

## 환경 변수 설정

### application.yml (api/src/main/resources)

```yaml
spring:
  application:
    name: auto-cert

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/autocert}
    username: ${DATABASE_USERNAME:autocert}
    password: ${DATABASE_PASSWORD:autocert}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

  flyway:
    enabled: true
    baseline-on-migrate: true

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

  cloud:
    vault:
      enabled: true
      host: ${VAULT_HOST:localhost}
      port: ${VAULT_PORT:8200}
      scheme: http
      authentication: TOKEN
      token: ${VAULT_TOKEN:root}

server:
  port: ${SERVER_PORT:8080}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

autocert:
  certificate:
    acme:
      directory-url: https://acme-v02.api.letsencrypt.org/directory
      account-email: ${ACME_EMAIL:admin@example.com}
    renewal:
      days-before-expiry: 30
      cron: "0 0 2 * * ?" # 매일 새벽 2시

  distribution:
    ssh:
      timeout: 30000
      max-retries: 3

  monitoring:
    alert:
      email:
        enabled: ${ALERT_EMAIL_ENABLED:false}
      slack:
        enabled: ${ALERT_SLACK_ENABLED:false}
        webhook-url: ${SLACK_WEBHOOK_URL:}

logging:
  level:
    com.hwgi.autocert: DEBUG
    org.springframework: INFO
```

## 개발 가이드

### 새 모듈 추가 방법

1. 모듈 디렉토리 생성
2. `build.gradle` 작성
3. `settings.gradle`에 모듈 추가
4. 패키지 구조 생성

### 코드 스타일

- Google Java Style Guide 또는 Spring Java Conventions 사용
- Checkstyle 적용 권장

```groovy
// build.gradle (root)
plugins {
    id 'checkstyle'
}

allprojects {
    apply plugin: 'checkstyle'

    checkstyle {
        toolVersion = '10.17.0'
        configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
    }
}
```

### Lombok 사용

```java
// Example Entity with Lombok
@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String domain;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Lob
    private String certificatePem;
}
```

### 테스트 전략

- **단위 테스트**: 각 모듈별 비즈니스 로직
- **통합 테스트**: API 엔드포인트
- **E2E 테스트**: 전체 워크플로우

```java
// Example Test
@SpringBootTest
class CertificateRenewalServiceTest {

    @Autowired
    private CertificateRenewalService service;

    @Test
    @DisplayName("인증서 만료 시 갱신 테스트")
    void shouldRenewCertificateWhenExpiring() {
        // given
        Certificate certificate = createTestCertificate();

        // when
        Certificate result = service.renew(certificate);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    private Certificate createTestCertificate() {
        return Certificate.builder()
                .domain("example.com")
                .expiresAt(LocalDateTime.now().plusDays(20))
                .build();
    }
}
```

## CI/CD 파이프라인

### GitHub Actions (.github/workflows/ci.yml)

```yaml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: 'gradle'

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build with Gradle
      run: ./gradlew clean build

    - name: Run tests
      run: ./gradlew test

    - name: Run checkstyle
      run: ./gradlew checkstyleMain checkstyleTest

    - name: Build Docker image
      run: docker build -t auto-cert:latest -f api/Dockerfile .

    - name: Upload test reports
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: test-reports
        path: '**/build/reports/tests/'
```

## 다음 단계

1. ✅ 프로젝트 구조 설계 완료
2. ✅ 공통 모듈 구현
3. 🔲 도메인 모델 정의
4. 🔲 데이터베이스 스키마 작성 (Flyway)
5. 🔲 Certificate Manager 구현
6. 🔲 API 엔드포인트 개발
7. 🔲 테스트 코드 작성
8. 🔲 Docker 컨테이너화
9. 🔲 CI/CD 파이프라인 구축

---

**문서 버전**: 1.0
**최종 수정일**: 2025-11-20

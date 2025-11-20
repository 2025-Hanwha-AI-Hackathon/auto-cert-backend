# Issue #9: 인증서 삭제 시 외래 키 제약 조건 위반 오류 및 전역 예외 처리 개선

**Labels:** `bug`, `enhancement`

## 문제 상황

### 1. 인증서 삭제 시 외래 키 제약 조건 위반
```
org.postgresql.util.PSQLException: ERROR: update or delete on table "certificates" violates foreign key constraint "deployments_certificate_id_fkey" on table "deployments"
```

인증서 삭제 API (`DELETE /api/v1/certificates/{id}`)를 호출할 때, `deployments` 테이블에서 해당 인증서를 참조하는 레코드가 있어 삭제가 실패함.

### 2. 전역 예외 처리 부재
```
auto-cert-app | ERROR o.a.c.c.C.[.[.[.[dispatcherServlet] - Servlet.service() for servlet [dispatcherServlet] threw exception
com.hwgi.autocert.common.exception.ResourceNotFoundException: 인증서를 찾을 수 없습니다: 34
```

존재하지 않는 리소스 조회 시 Spring의 기본 에러 핸들링만 사용하여 불필요한 ERROR 로그가 출력되고, 클라이언트에게 일관된 API 응답 형식이 제공되지 않음.

## 해결 방법

### 1. 인증서 삭제 로직 수정
`CertificateService.delete()` 메서드에서 인증서 삭제 전에 연관된 배포 이력을 먼저 삭제:

```java
@Transactional
public void delete(Long id) {
    // 1. 연관된 Deployment 레코드 삭제 (외래 키 제약 조건 위반 방지)
    List<Deployment> deployments = deploymentRepository
        .findByCertificateIdOrderByDeployedAtDesc(id, Pageable.unpaged())
        .getContent();

    if (!deployments.isEmpty()) {
        deploymentRepository.deleteAll(deployments);
    }

    // 2. 인증서 삭제
    Certificate certificate = findById(id);
    certificateRepository.delete(certificate);
}
```

### 2. 전역 예외 핸들러 추가
`GlobalExceptionHandler` 클래스 생성하여 일관된 예외 처리:

**처리하는 예외:**
- `ResourceNotFoundException` → 404 Not Found
- `IllegalArgumentException` → 400 Bad Request
- `IllegalStateException` → 409 Conflict
- `MethodArgumentNotValidException` → 400 Bad Request (검증 실패)
- `RuntimeException` / `Exception` → 500 Internal Server Error

**응답 형식:**
```json
{
  "success": false,
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "인증서를 찾을 수 없습니다: 34",
  "timestamp": "2025-11-20T19:41:24"
}
```

## 변경 사항

### 수정된 파일
- `certificate-manager/src/main/java/com/hwgi/autocert/certificate/service/CertificateService.java`
  - `DeploymentRepository` 의존성 추가
  - `delete()` 메서드 수정: 배포 이력 선삭제 로직 추가
  - 관련 import 추가 (`DeploymentRepository`, `Deployment`)

### 추가된 파일
- `api/src/main/java/com/hwgi/autocert/api/exception/GlobalExceptionHandler.java`
  - `@RestControllerAdvice` 전역 예외 핸들러 구현
  - 모든 예외를 일관된 `ApiResponse` 형식으로 반환
  - 예외별 적절한 HTTP 상태 코드 및 로그 레벨 설정

## 테스트 시나리오

1. **인증서 삭제 테스트**
   - 배포 이력이 있는 인증서 삭제 시 정상 삭제 확인
   - 배포 이력이 없는 인증서 삭제 시 정상 삭제 확인
   - 존재하지 않는 인증서 삭제 시도 시 404 응답 확인

2. **예외 처리 테스트**
   - 존재하지 않는 인증서 조회 시 404 응답 및 일관된 JSON 형식 확인
   - 잘못된 요청 파라미터 전달 시 400 응답 확인
   - 검증 실패 시 필드별 에러 정보 반환 확인
   - 중복 도메인 생성 시도 시 400 응답 확인

## 영향 범위

- ✅ 인증서 삭제 API의 안정성 향상
- ✅ 모든 REST API의 에러 응답 일관성 확보
- ✅ 불필요한 ERROR 로그 감소 (정상적인 404는 WARN 레벨로 기록)
- ✅ 클라이언트의 에러 핸들링 개선 (일관된 응답 형식)

## 브랜치

`feature/#9-fix-delete-certificate`

## 체크리스트

- [x] 인증서 삭제 로직 수정
- [x] 전역 예외 핸들러 구현
- [x] 빌드 성공 확인
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] API 문서 업데이트 (Swagger)
- [ ] PR 생성

package com.hwgi.autocert.api.controller;

import com.hwgi.autocert.api.dto.request.CertificateCreateRequest;
import com.hwgi.autocert.api.dto.request.CertificateUpdateRequest;
import com.hwgi.autocert.api.dto.response.CertificateDeployResponse;
import com.hwgi.autocert.api.dto.response.CertificateResponse;
import com.hwgi.autocert.api.dto.response.PageResponse;
import com.hwgi.autocert.certificate.service.CertificateService;
import com.hwgi.autocert.common.dto.ApiResponse;
import com.hwgi.autocert.domain.model.Certificate;
import com.hwgi.autocert.domain.model.Deployment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 인증서 관리 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificate", description = "인증서 관리 API")
public class CertificateController {

    private final CertificateService certificateService;

    @Operation(summary = "인증서 목록 조회", description = "페이지네이션을 지원하는 인증서 목록 조회")
    @GetMapping
    public ApiResponse<PageResponse<CertificateResponse>> getCertificates(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Get certificates list - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Certificate> page = certificateService.findAll(pageable);
        PageResponse<CertificateResponse> response = PageResponse.from(
            page.map(certificate -> {
                String latestDeploymentStatus = certificateService.getLatestDeploymentStatus(certificate.getId());
                return CertificateResponse.from(certificate, latestDeploymentStatus);
            })
        );
        return ApiResponse.success(response, "인증서 목록 조회 성공");
    }

    @Operation(summary = "인증서 상세 조회", description = "ID로 특정 인증서 조회")
    @GetMapping("/{id}")
    public ApiResponse<CertificateResponse> getCertificate(@PathVariable Long id) {
        log.info("Get certificate by id: {}", id);
        
        Certificate certificate = certificateService.findById(id);
        String latestDeploymentStatus = certificateService.getLatestDeploymentStatus(id);
        CertificateResponse response = CertificateResponse.from(certificate, latestDeploymentStatus);
        return ApiResponse.success(response, "인증서 조회 성공");
    }

    @Operation(summary = "인증서 생성", description = "새로운 인증서 발급 요청")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CertificateResponse> createCertificate(
            @Valid @RequestBody CertificateCreateRequest request) {
        log.info("Create certificate for domain: {}, autoDeploy: {}", 
            request.getDomain(), request.getAutoDeploy());
        
        Certificate certificate = certificateService.create(
                request.getServerId(),
                request.getDomain(),
                request.getChallengeType(),
                request.getAdmin(),
                request.getAlertDaysBeforeExpiry(),
                request.getAutoDeploy());
        String latestDeploymentStatus = certificateService.getLatestDeploymentStatus(certificate.getId());
        CertificateResponse response = CertificateResponse.from(certificate, latestDeploymentStatus);
        return ApiResponse.success(response, "인증서가 생성 성공");
    }

    @Operation(summary = "인증서 수정", description = "인증서 정보 수정 (모든 필드 수정 가능)")
    @PutMapping("/{id}")
    public ApiResponse<CertificateResponse> updateCertificate(
            @PathVariable Long id,
            @Valid @RequestBody CertificateUpdateRequest request) {
        log.info("Update certificate: {}", id);
        
        Certificate updated = certificateService.update(
            id,
            request.getDomain(),
            request.getServerId(),
            request.getIssuedAt(),
            request.getExpiresAt(),
            request.getStatus(),
            request.getCertificatePem(),
            request.getPrivateKeyPem(),
            request.getChainPem(),
            request.getPassword(),
            request.getAdmin(),
            request.getAlertDaysBeforeExpiry(),
            request.getAutoDeploy()
        );
        
        String latestDeploymentStatus = certificateService.getLatestDeploymentStatus(id);
        CertificateResponse response = CertificateResponse.from(updated, latestDeploymentStatus);
        return ApiResponse.success(response, "인증서가 수정되었습니다");
    }

    @Operation(summary = "인증서 갱신", description = "만료 예정 인증서 수동 갱신")
    @PostMapping("/{id}/renew")
    public ApiResponse<CertificateResponse> renewCertificate(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") Boolean autoDeploy) {
        log.info("Renew certificate: {}, autoDeploy: {}", id, autoDeploy);
        
        Certificate certificate = certificateService.renew(id, autoDeploy);
        String latestDeploymentStatus = certificateService.getLatestDeploymentStatus(id);
        CertificateResponse response = CertificateResponse.from(certificate, latestDeploymentStatus);
        return ApiResponse.success(response, "인증서 갱신 성공");
    }

    @Operation(summary = "인증서 수동 배포", description = "저장된 인증서를 서버에 수동으로 배포")
    @PostMapping("/{id}/deploy")
    public ApiResponse<CertificateDeployResponse> deployCertificate(
            @PathVariable Long id) {
        log.info("Deploy certificate: {}", id);
        
        Deployment deployment = certificateService.deployManually(id);
        CertificateDeployResponse response = CertificateDeployResponse.from(deployment);
        return ApiResponse.success(response, "인증서 배포 성공");
    }

    @Operation(summary = "인증서 삭제", description = "인증서 삭제 (주의: 복구 불가)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteCertificate(@PathVariable Long id) {
        log.info("Delete certificate: {}", id);
        
        certificateService.delete(id);
        return ApiResponse.success(null, "인증서가 삭제되었습니다");
    }
}

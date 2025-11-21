package com.hwgi.autocert.api.controller;

import com.hwgi.autocert.api.dto.request.CertificateValidationRequest;
import com.hwgi.autocert.api.dto.response.CertificateValidationResponse;
import com.hwgi.autocert.certificate.validation.CertificateValidationService;
import com.hwgi.autocert.domain.model.CertificateValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증서 검증 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificate Validation", description = "인증서 검증 API")
public class CertificateValidationController {
    
    private final CertificateValidationService validationService;
    
    /**
     * 저장된 인증서 검증
     */
    @Operation(
        summary = "저장된 인증서 검증",
        description = "데이터베이스에 저장된 인증서의 유효성을 검증합니다. 서명, 유효기간, 체인, 폐기 여부, 도메인, 키 사용 등을 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "검증 완료",
            content = @Content(schema = @Schema(implementation = CertificateValidationResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "인증서를 찾을 수 없음"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류"
        )
    })
    @GetMapping("/{id}/validate")
    public ResponseEntity<CertificateValidationResponse> validateStoredCertificate(
            @Parameter(description = "인증서 ID", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("Validating certificate with ID: {}", id);
        
        CertificateValidationResult result = validationService.validateCertificate(id);
        CertificateValidationResponse response = CertificateValidationResponse.from(result);
        
        log.info("Validation completed for certificate ID {}: {}", id, result.isValid() ? "VALID" : "INVALID");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * PEM 형식의 인증서 직접 검증
     */
    @Operation(
        summary = "PEM 인증서 직접 검증",
        description = "PEM 형식의 인증서를 직접 제출하여 유효성을 검증합니다. 인증서 체인도 함께 제공할 수 있습니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "검증 완료",
            content = @Content(schema = @Schema(implementation = CertificateValidationResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (PEM 형식 오류)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류"
        )
    })
    @PostMapping("/validate")
    public ResponseEntity<CertificateValidationResponse> validatePemCertificate(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "검증할 인증서 정보",
                required = true,
                content = @Content(schema = @Schema(implementation = CertificateValidationRequest.class))
            )
            @RequestBody CertificateValidationRequest request
    ) {
        log.info("Validating PEM certificate");
        
        if (request.getCertificatePem() == null || request.getCertificatePem().trim().isEmpty()) {
            log.error("Certificate PEM is required");
            throw new IllegalArgumentException("Certificate PEM is required");
        }
        
        CertificateValidationResult result = validationService.validateCertificatePem(
                request.getCertificatePem(),
                request.getCertificateChainPem()
        );
        
        CertificateValidationResponse response = CertificateValidationResponse.from(result);
        
        log.info("Validation completed for PEM certificate: {}", result.isValid() ? "VALID" : "INVALID");
        
        return ResponseEntity.ok(response);
    }
}

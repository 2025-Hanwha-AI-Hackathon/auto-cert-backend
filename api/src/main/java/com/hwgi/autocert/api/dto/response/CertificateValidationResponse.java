package com.hwgi.autocert.api.dto.response;

import com.hwgi.autocert.domain.model.CertificateValidationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 인증서 검증 결과 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인증서 전체 검증 결과")
public class CertificateValidationResponse {
    
    @Schema(description = "전체 검증 통과 여부", example = "true")
    private boolean valid;
    
    @Schema(description = "검증 수행 시각", example = "2025-11-21T10:30:00")
    private LocalDateTime validatedAt;
    
    @Schema(description = "서명 검증 결과")
    private ValidationCheckResponse signatureCheck;
    
    @Schema(description = "유효기간 검증 결과")
    private ValidationCheckResponse validityCheck;
    
    @Schema(description = "인증서 체인 검증 결과")
    private ValidationCheckResponse chainCheck;
    
    @Schema(description = "폐기 여부 검증 결과")
    private ValidationCheckResponse revocationCheck;
    
    @Schema(description = "도메인 일치 검증 결과")
    private ValidationCheckResponse domainCheck;
    
    @Schema(description = "키 사용 검증 결과")
    private ValidationCheckResponse keyUsageCheck;
    
    @Schema(description = "경고 메시지 목록")
    private List<String> warnings;
    
    @Schema(description = "에러 메시지 목록")
    private List<String> errors;
    
    /**
     * Domain 모델을 Response DTO로 변환
     */
    public static CertificateValidationResponse from(CertificateValidationResult validationResult) {
        return CertificateValidationResponse.builder()
                .valid(validationResult.isValid())
                .validatedAt(validationResult.getValidatedAt())
                .signatureCheck(ValidationCheckResponse.from(validationResult.getSignatureCheck()))
                .validityCheck(ValidationCheckResponse.from(validationResult.getValidityCheck()))
                .chainCheck(ValidationCheckResponse.from(validationResult.getChainCheck()))
                .revocationCheck(ValidationCheckResponse.from(validationResult.getRevocationCheck()))
                .domainCheck(ValidationCheckResponse.from(validationResult.getDomainCheck()))
                .keyUsageCheck(ValidationCheckResponse.from(validationResult.getKeyUsageCheck()))
                .warnings(validationResult.getWarnings())
                .errors(validationResult.getErrors())
                .build();
    }
}

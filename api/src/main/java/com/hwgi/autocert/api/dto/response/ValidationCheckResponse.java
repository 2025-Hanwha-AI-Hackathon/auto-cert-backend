package com.hwgi.autocert.api.dto.response;

import com.hwgi.autocert.domain.model.ValidationCheckResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 개별 검증 항목 결과 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "개별 검증 항목 결과")
public class ValidationCheckResponse {
    
    @Schema(description = "검증 통과 여부", example = "true")
    private boolean valid;
    
    @Schema(description = "검증 결과 메시지", example = "Certificate is valid (30 days remaining)")
    private String message;
    
    @Schema(description = "검증 세부 정보", example = "NotBefore: 2025-01-01, NotAfter: 2025-12-31")
    private String details;
    
    @Schema(description = "에러 코드 (실패 시)", example = "CERTIFICATE_EXPIRED")
    private String errorCode;
    
    /**
     * Domain 모델을 Response DTO로 변환
     */
    public static ValidationCheckResponse from(ValidationCheckResult checkResult) {
        if (checkResult == null) {
            return null;
        }
        
        return ValidationCheckResponse.builder()
                .valid(checkResult.isValid())
                .message(checkResult.getMessage())
                .details(checkResult.getDetails())
                .errorCode(checkResult.getErrorCode())
                .build();
    }
}

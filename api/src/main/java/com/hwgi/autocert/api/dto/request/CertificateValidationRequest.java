package com.hwgi.autocert.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증서 검증 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인증서 검증 요청")
public class CertificateValidationRequest {
    
    @Schema(description = "PEM 형식의 인증서 (선택사항, certificateId가 없을 경우 필수)", example = "-----BEGIN CERTIFICATE-----\n...")
    private String certificatePem;
    
    @Schema(description = "PEM 형식의 인증서 체인 (선택사항)", example = "-----BEGIN CERTIFICATE-----\n...")
    private String certificateChainPem;
}

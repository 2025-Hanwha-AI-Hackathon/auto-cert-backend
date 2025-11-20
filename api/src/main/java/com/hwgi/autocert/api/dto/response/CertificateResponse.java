package com.hwgi.autocert.api.dto.response;

import com.hwgi.autocert.domain.model.Certificate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 인증서 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponse {

    private Long id;
    private String domain;
    private String issuer;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private String status;
    private Integer renewalAttempts;
    private String lastError;
    private String admin;
    private Integer alertDaysBeforeExpiry;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity to Response DTO
     */
    public static CertificateResponse from(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .domain(certificate.getDomain())
                .issuedAt(certificate.getIssuedAt())
                .expiresAt(certificate.getExpiresAt())
                .status(certificate.getStatus().name())
                .admin(certificate.getAdmin())
                .alertDaysBeforeExpiry(certificate.getAlertDaysBeforeExpiry())
                .createdAt(certificate.getCreatedAt())
                .updatedAt(certificate.getUpdatedAt())
                .build();
    }
}

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
    private Long serverId;
    private String domain;
    private String issuer;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private String status;
    private Integer renewalAttempts;
    private String lastError;
    private String admin;
    private Integer alertDaysBeforeExpiry;
    private Boolean autoDeploy;
    private String latestDeploymentStatus;  // 최신 배포 상태
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity to Response DTO
     */
    public static CertificateResponse from(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .serverId(certificate.getServer() != null ? certificate.getServer().getId() : null)
                .domain(certificate.getDomain())
                .issuedAt(certificate.getIssuedAt())
                .expiresAt(certificate.getExpiresAt())
                .status(certificate.getStatus().name())
                .admin(certificate.getAdmin())
                .alertDaysBeforeExpiry(certificate.getAlertDaysBeforeExpiry())
                .autoDeploy(certificate.getAutoDeploy())
                .createdAt(certificate.getCreatedAt())
                .updatedAt(certificate.getUpdatedAt())
                .build();
    }

    /**
     * Entity to Response DTO with deployment status
     */
    public static CertificateResponse from(Certificate certificate, String latestDeploymentStatus) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .serverId(certificate.getServer() != null ? certificate.getServer().getId() : null)
                .domain(certificate.getDomain())
                .issuedAt(certificate.getIssuedAt())
                .expiresAt(certificate.getExpiresAt())
                .status(certificate.getStatus().name())
                .admin(certificate.getAdmin())
                .alertDaysBeforeExpiry(certificate.getAlertDaysBeforeExpiry())
                .autoDeploy(certificate.getAutoDeploy())
                .latestDeploymentStatus(latestDeploymentStatus)
                .createdAt(certificate.getCreatedAt())
                .updatedAt(certificate.getUpdatedAt())
                .build();
    }
}

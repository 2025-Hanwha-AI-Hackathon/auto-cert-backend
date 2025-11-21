package com.hwgi.autocert.api.dto.response;

import com.hwgi.autocert.domain.model.Deployment;
import com.hwgi.autocert.domain.model.DeploymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 인증서 배포 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인증서 배포 응답")
public class CertificateDeployResponse {

    @Schema(description = "배포 ID", example = "1")
    private Long deploymentId;

    @Schema(description = "인증서 ID", example = "1")
    private Long certificateId;

    @Schema(description = "인증서 도메인", example = "example.com")
    private String certificateDomain;

    @Schema(description = "서버 ID", example = "1")
    private Long serverId;

    @Schema(description = "서버 이름", example = "Web Server 01")
    private String serverName;

    @Schema(description = "서버 IP 주소", example = "192.168.1.100")
    private String serverIp;

    @Schema(description = "배포 상태", example = "SUCCESS")
    private DeploymentStatus status;

    @Schema(description = "배포 경로", example = "/etc/nginx/ssl")
    private String deploymentPath;

    @Schema(description = "배포 시각")
    private LocalDateTime deployedAt;

    @Schema(description = "배포 메시지", example = "Successfully deployed certificate files")
    private String message;

    @Schema(description = "배포 소요 시간 (밀리초)", example = "1234")
    private Long durationMs;

    /**
     * Deployment 엔티티를 DTO로 변환
     *
     * @param deployment 배포 엔티티
     * @return 배포 응답 DTO
     */
    public static CertificateDeployResponse from(Deployment deployment) {
        return CertificateDeployResponse.builder()
                .deploymentId(deployment.getId())
                .certificateId(deployment.getCertificate().getId())
                .certificateDomain(deployment.getCertificate().getDomain())
                .serverId(deployment.getServer().getId())
                .serverName(deployment.getServer().getName())
                .serverIp(deployment.getServer().getIpAddress())
                .status(deployment.getStatus())
                .deploymentPath(deployment.getDeploymentPath())
                .deployedAt(deployment.getDeployedAt())
                .message(deployment.getMessage())
                .durationMs(deployment.getDurationMs())
                .build();
    }
}

package com.hwgi.autocert.api.dto.request;

import com.hwgi.autocert.domain.model.CertificateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 인증서 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class CertificateUpdateRequest {

    @Schema(description = "도메인 이름")
    private String domain;

    @Schema(description = "서버 ID")
    private Long serverId;

    @Schema(description = "발급 일시")
    private LocalDateTime issuedAt;

    @Schema(description = "만료 일시")
    private LocalDateTime expiresAt;

    @Schema(description = "인증서 상태 (ACTIVE, EXPIRED, REVOKED, PENDING)")
    private CertificateStatus status;

    @Schema(description = "인증서 PEM 형식 데이터")
    private String certificatePem;

    @Schema(description = "개인키 PEM 형식 데이터")
    private String privateKeyPem;

    @Schema(description = "체인 PEM 형식 데이터")
    private String chainPem;

    @Schema(description = "인증서 비밀번호")
    private String password;

    @Schema(description = "인증서 관리자 또는 담당자")
    private String admin;

    @Schema(description = "만료 전 알림 일수", example = "7")
    @Min(value = 1, message = "알림 일수는 1일 이상이어야 합니다")
    private Integer alertDaysBeforeExpiry;

    @Schema(description = "서버에 자동 배포 여부", example = "false")
    private Boolean autoDeploy;
}

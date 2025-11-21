package com.hwgi.autocert.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증서 배포 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인증서 배포 요청")
public class CertificateDeployRequest {

    @Schema(description = "인증서 ID", example = "1", required = true)
    @NotNull(message = "인증서 ID는 필수입니다")
    private Long certificateId;

    @Schema(description = "서버에 배포 후 자동 재기동 여부", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean autoReload = true;
}

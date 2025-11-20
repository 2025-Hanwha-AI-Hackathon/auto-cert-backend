package com.hwgi.autocert.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증서 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
public class CertificateCreateRequest {

    @Schema(example = "example.com", description = "인증서를 발급받을 도메인")
    @NotBlank(message = "도메인은 필수입니다")
    @Pattern(regexp = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$",
            message = "올바른 도메인 형식이 아닙니다")
    private String domain;

    private String challengeType;

    @Schema(description = "인증서 관리자 또는 담당자 (선택사항)")
    private String admin;

    @Schema(description = "만료 전 알림 일수 (기본값: 7일)", example = "7")
    @Min(value = 1, message = "알림 일수는 1일 이상이어야 합니다")
    private Integer alertDaysBeforeExpiry;
}

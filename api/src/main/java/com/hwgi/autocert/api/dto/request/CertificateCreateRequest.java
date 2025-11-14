package com.hwgi.autocert.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
}

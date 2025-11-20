package com.hwgi.autocert.server.dto;

import com.hwgi.autocert.common.constants.WebServerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서버 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class ServerUpdateRequest {

    @Schema(example = "Web Server 1", description = "서버 이름")
    private String name;

    @Schema(example = "192.168.1.100", description = "서버 IP 주소")
    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
            message = "올바른 IP 주소 형식이 아닙니다")
    private String ipAddress;

    @Schema(example = "22", description = "SSH 포트")
    @Min(value = 1, message = "포트는 1 이상이어야 합니다")
    private Integer port;

    @Schema(description = "웹서버 타입", example = "nginx")
    private WebServerType webServerType;

    @Schema(example = "Production web server", description = "서버 설명")
    private String description;

    @Schema(example = "ubuntu", description = "SSH 사용자명")
    private String username;

    @Schema(example = "password123", description = "SSH 비밀번호")
    private String password;

    @Schema(example = "/etc/nginx/ssl", description = "인증서 배포 경로")
    private String deployPath;
}


package com.hwgi.autocert.api.dto.response;

import com.hwgi.autocert.domain.model.Server;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 서버 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponse {

    private Long id;
    private String name;
    private String ipAddress;
    private Integer port;
    private String webServerType;
    private String description;
    private String username;
    private String deployPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity to Response DTO
     * 보안상 password는 응답에 포함하지 않음
     */
    public static ServerResponse from(Server server) {
        return ServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .ipAddress(server.getIpAddress())
                .port(server.getPort())
                .webServerType(server.getWebServerType().getCode())
                .description(server.getDescription())
                .username(server.getUsername())
                .deployPath(server.getDeployPath())
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }
}

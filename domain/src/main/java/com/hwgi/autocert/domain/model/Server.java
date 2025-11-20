package com.hwgi.autocert.domain.model;

import com.hwgi.autocert.common.constants.WebServerType;
import com.hwgi.autocert.domain.converter.WebServerTypeConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "ip_address", nullable = false, unique = true)
    private String ipAddress;

    @Column(nullable = false)
    private Integer port;

    @Column(name = "web_server_type", nullable = false)
    @Convert(converter = WebServerTypeConverter.class)
    private WebServerType webServerType;

    @Column
    private String description;

    @Column(nullable = false)
    private String username;

    @Column
    private String password;

    @Column(nullable = false)
    private String deployPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

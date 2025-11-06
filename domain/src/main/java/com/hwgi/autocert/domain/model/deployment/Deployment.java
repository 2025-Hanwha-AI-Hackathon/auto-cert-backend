package com.hwgi.autocert.domain.model.deployment;

import com.hwgi.autocert.domain.model.certificate.Certificate;
import com.hwgi.autocert.domain.model.server.Server;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "deployments")
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false)
    private Certificate certificate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentStatus status;

    @Column(name = "deployed_at")
    private LocalDateTime deployedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column
    private String details;

    public enum DeploymentStatus {
        PENDING,
        IN_PROGRESS,
        SUCCESS,
        FAILED
    }

    // Getters and Setters
}

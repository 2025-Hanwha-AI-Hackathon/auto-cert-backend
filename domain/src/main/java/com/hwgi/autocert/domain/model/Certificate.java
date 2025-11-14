package com.hwgi.autocert.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificateStatus status;

    @Lob
    @Column(name = "certificate_pem", columnDefinition = "TEXT")
    private String certificatePem;

    @Lob
    @Column(name = "private_key_pem", columnDefinition = "TEXT")
    private String privateKeyPem;

    @Lob
    @Column(name = "chain_pem", columnDefinition = "TEXT")
    private String chainPem;

    @Column
    private String password;

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

package com.hwgi.autocert.domain.model.certificate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "serial_number", unique = true)
    private String serialNumber;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Lob
    @Column(name = "certificate_pem", columnDefinition = "TEXT")
    private String certificatePem;

    @Lob
    @Column(name = "private_key_pem", columnDefinition = "TEXT")
    private String privateKeyPem;

    // Getters and Setters
}

package com.hwgi.autocert.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ACME 계정 엔티티
 * 
 * Let's Encrypt 등 ACME 프로토콜을 사용하는 CA와 통신하기 위한 계정 정보 저장
 */
@Entity
@Table(name = "acme_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcmeAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ACME 계정 이메일 (연락처)
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * ACME 서버 URL (예: https://acme-v02.api.letsencrypt.org/directory)
     */
    @Column(name = "acme_server_url", nullable = false)
    private String acmeServerUrl;

    /**
     * ACME 계정 URL (CA가 발급한 계정 리소스 URL)
     */
    @Column(name = "account_url")
    private String accountUrl;

    /**
     * 계정 키페어 (암호화된 개인키 - Base64 인코딩)
     * 보안: Vault 또는 암호화 저장 권장
     */
    @Lob
    @Column(name = "private_key_pem", columnDefinition = "TEXT", nullable = false)
    private String privateKeyPem;

    /**
     * 공개키 (PEM 형식)
     */
    @Lob
    @Column(name = "public_key_pem", columnDefinition = "TEXT")
    private String publicKeyPem;

    /**
     * 계정 상태 (ACTIVE, DEACTIVATED, REVOKED)
     */
    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * 키 알고리즘 타입 (RSA, ECDSA)
     */
    @Column(name = "key_algorithm")
    @Builder.Default
    private String keyAlgorithm = "RSA";

    /**
     * 키 사이즈 (RSA: 2048/4096, ECDSA: 256/384)
     */
    @Column(name = "key_size")
    @Builder.Default
    private Integer keySize = 4096;

    /**
     * 서비스 약관 동의 여부
     */
    @Column(name = "terms_agreed")
    @Builder.Default
    private Boolean termsAgreed = false;

    /**
     * 계정 생성 일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 계정 수정 일시
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 마지막 사용 일시
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 마지막 사용 시간 업데이트
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}

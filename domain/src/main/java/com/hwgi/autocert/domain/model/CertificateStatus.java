package com.hwgi.autocert.domain.model;

/**
 * 인증서 상태를 나타내는 Enum
 */
public enum CertificateStatus {
    /**
     * 발급 대기 중
     */
    PENDING("발급 대기"),

    /**
     * 발급 중
     */
    ISSUING("발급 중"),

    /**
     * 활성 상태 (정상 사용 가능)
     */
    ACTIVE("활성"),

    /**
     * 만료 임박 (갱신 필요)
     */
    EXPIRING_SOON("만료 임박"),

    /**
     * 만료됨
     */
    EXPIRED("만료됨"),

    /**
     * 갱신 중
     */
    RENEWING("갱신 중"),

    /**
     * 폐기됨
     */
    REVOKED("폐기됨"),

    /**
     * 실패 (발급 또는 갱신 실패)
     */
    FAILED("실패"),

    /**
     * 비활성화됨
     */
    INACTIVE("비활성");

    private final String description;

    CertificateStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 인증서가 유효한 상태인지 확인
     * 
     * @return 유효한 상태면 true
     */
    public boolean isValid() {
        return this == ACTIVE || this == EXPIRING_SOON;
    }

    /**
     * 인증서가 갱신 가능한 상태인지 확인
     * 
     * @return 갱신 가능한 상태면 true
     */
    public boolean isRenewable() {
        return this == ACTIVE || this == EXPIRING_SOON || this == EXPIRED;
    }

    /**
     * 인증서가 처리 중인 상태인지 확인
     * 
     * @return 처리 중인 상태면 true
     */
    public boolean isProcessing() {
        return this == PENDING || this == ISSUING || this == RENEWING;
    }
}

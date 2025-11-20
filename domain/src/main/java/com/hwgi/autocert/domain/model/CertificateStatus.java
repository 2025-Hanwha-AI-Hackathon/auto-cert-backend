package com.hwgi.autocert.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 인증서 상태를 나타내는 Enum
 */
@Getter
@RequiredArgsConstructor
public enum CertificateStatus {
    /**
     * 발급 대기 중
     */
    PENDING("pending", "발급 대기"),

    /**
     * 발급 중
     */
    ISSUING("issuing", "발급 중"),

    /**
     * 활성 상태 (정상 사용 가능)
     */
    ACTIVE("active", "활성"),

    /**
     * 만료 임박 (갱신 필요)
     */
    EXPIRING_SOON("expiring_soon", "만료 임박"),

    /**
     * 만료됨
     */
    EXPIRED("expired", "만료됨"),

    /**
     * 갱신 중
     */
    RENEWING("renewing", "갱신 중"),

    /**
     * 폐기됨
     */
    REVOKED("revoked", "폐기됨"),

    /**
     * 실패 (발급 또는 갱신 실패)
     */
    FAILED("failed", "실패"),

    /**
     * 비활성화됨
     */
    INACTIVE("inactive", "비활성");

    @JsonValue
    private final String code;
    private final String description;

    /**
     * code 문자열로부터 enum 조회
     */
    @JsonCreator
    public static CertificateStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (CertificateStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("지원하지 않는 인증서 상태입니다: " + code);
    }

    /**
     * 문자열이 유효한 인증서 상태 코드인지 확인
     */
    public static boolean isValidCode(String code) {
        if (code == null) {
            return false;
        }
        
        for (CertificateStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return true;
            }
        }
        
        return false;
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

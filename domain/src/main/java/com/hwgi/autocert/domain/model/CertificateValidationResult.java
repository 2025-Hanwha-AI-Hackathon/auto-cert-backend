package com.hwgi.autocert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 인증서 전체 검증 결과를 나타내는 클래스
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateValidationResult {
    
    /**
     * 전체 검증 통과 여부 (모든 검증 항목이 통과해야 true)
     */
    private boolean valid;
    
    /**
     * 검증 수행 시각
     */
    private LocalDateTime validatedAt;
    
    /**
     * 서명 검증 결과
     */
    private ValidationCheckResult signatureCheck;
    
    /**
     * 유효기간 검증 결과
     */
    private ValidationCheckResult validityCheck;
    
    /**
     * 인증서 체인 검증 결과
     */
    private ValidationCheckResult chainCheck;
    
    /**
     * 폐기 여부 검증 결과 (CRL/OCSP)
     */
    private ValidationCheckResult revocationCheck;
    
    /**
     * 도메인 일치 검증 결과
     */
    private ValidationCheckResult domainCheck;
    
    /**
     * 키 사용 검증 결과
     */
    private ValidationCheckResult keyUsageCheck;
    
    /**
     * 경고 메시지 목록
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    /**
     * 에러 메시지 목록
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    /**
     * 경고 추가
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
    
    /**
     * 에러 추가
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }
    
    /**
     * 모든 검증 항목의 결과를 기반으로 전체 유효성 판단
     */
    public boolean isAllValid() {
        return signatureCheck != null && signatureCheck.isValid() &&
               validityCheck != null && validityCheck.isValid() &&
               chainCheck != null && chainCheck.isValid() &&
               revocationCheck != null && revocationCheck.isValid() &&
               domainCheck != null && domainCheck.isValid() &&
               keyUsageCheck != null && keyUsageCheck.isValid();
    }
}

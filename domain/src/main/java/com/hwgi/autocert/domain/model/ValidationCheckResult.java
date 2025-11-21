package com.hwgi.autocert.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 개별 검증 항목의 결과를 나타내는 클래스
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationCheckResult {
    
    /**
     * 검증 통과 여부
     */
    private boolean valid;
    
    /**
     * 검증 결과 메시지
     */
    private String message;
    
    /**
     * 검증 세부 정보 (JSON 형태의 추가 정보)
     */
    private String details;
    
    /**
     * 검증 실패 시 에러 코드
     */
    private String errorCode;
    
    /**
     * 성공 결과 생성
     */
    public static ValidationCheckResult success(String message) {
        return ValidationCheckResult.builder()
                .valid(true)
                .message(message)
                .build();
    }
    
    /**
     * 성공 결과 생성 (세부 정보 포함)
     */
    public static ValidationCheckResult success(String message, String details) {
        return ValidationCheckResult.builder()
                .valid(true)
                .message(message)
                .details(details)
                .build();
    }
    
    /**
     * 실패 결과 생성
     */
    public static ValidationCheckResult failure(String message, String errorCode) {
        return ValidationCheckResult.builder()
                .valid(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
    
    /**
     * 실패 결과 생성 (세부 정보 포함)
     */
    public static ValidationCheckResult failure(String message, String errorCode, String details) {
        return ValidationCheckResult.builder()
                .valid(false)
                .message(message)
                .errorCode(errorCode)
                .details(details)
                .build();
    }
}

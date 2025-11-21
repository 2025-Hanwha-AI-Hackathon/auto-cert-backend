package com.hwgi.autocert.notification.service;

import com.hwgi.autocert.domain.model.Certificate;

import java.util.List;

/**
 * 이메일 알림 서비스 인터페이스
 */
public interface EmailService {
    
    /**
     * 인증서 생성 완료 이메일 발송
     * 
     * @param certificate 생성된 인증서
     */
    void sendCertificateCreated(Certificate certificate);
    
    /**
     * 인증서 갱신 완료 이메일 발송
     * 
     * @param certificate 갱신된 인증서
     */
    void sendCertificateRenewed(Certificate certificate);
    
    /**
     * 인증서 삭제 완료 이메일 발송
     * 
     * @param domain 삭제된 인증서의 도메인
     */
    void sendCertificateDeleted(String domain);
    
    /**
     * 만료 임박 인증서 알림 이메일 발송
     * 
     * @param certificates 만료 임박 인증서 목록
     */
    void sendExpiringCertificatesAlert(List<Certificate> certificates);
}

package com.hwgi.autocert.certificate.validation;

import com.hwgi.autocert.domain.model.ValidationCheckResult;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * 인증서 검증을 위한 인터페이스
 */
public interface CertificateValidator {
    
    /**
     * 인증서 검증 수행
     *
     * @param certificate 검증할 인증서
     * @param certificateChain 인증서 체인 (선택사항)
     * @return 검증 결과
     */
    ValidationCheckResult validate(X509Certificate certificate, List<X509Certificate> certificateChain);
}

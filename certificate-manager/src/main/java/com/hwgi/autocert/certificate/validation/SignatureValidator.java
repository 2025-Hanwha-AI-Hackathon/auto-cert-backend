package com.hwgi.autocert.certificate.validation;

import com.hwgi.autocert.domain.model.ValidationCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * 인증서 서명 검증
 * - 인증서가 신뢰할 수 있는 CA에 의해 서명되었는지 확인
 */
@Slf4j
@Component
public class SignatureValidator implements CertificateValidator {
    
    @Override
    public ValidationCheckResult validate(X509Certificate certificate, List<X509Certificate> certificateChain) {
        try {
            // 자체 서명 인증서인 경우
            if (isSelfSigned(certificate)) {
                log.warn("Self-signed certificate detected: {}", certificate.getSubjectX500Principal());
                return ValidationCheckResult.success(
                        "Self-signed certificate (not verified against CA)",
                        "Self-signed: true"
                );
            }
            
            // 체인이 있는 경우, 발급자 인증서로 서명 검증
            if (certificateChain != null && certificateChain.size() > 1) {
                X509Certificate issuerCert = certificateChain.get(1);
                certificate.verify(issuerCert.getPublicKey());
                
                return ValidationCheckResult.success(
                        "Certificate signature verified by issuer: " + issuerCert.getSubjectX500Principal(),
                        "Issuer: " + issuerCert.getSubjectX500Principal()
                );
            }
            
            // 체인이 없는 경우, 자체 공개키로 검증 시도 (자체 서명 확인)
            certificate.verify(certificate.getPublicKey());
            
            return ValidationCheckResult.success(
                    "Certificate signature is mathematically valid",
                    "Verified with own public key"
            );
            
        } catch (CertificateException e) {
            log.error("Certificate signature validation failed: {}", e.getMessage());
            return ValidationCheckResult.failure(
                    "Certificate signature is invalid: " + e.getMessage(),
                    "INVALID_SIGNATURE",
                    e.getClass().getSimpleName()
            );
        } catch (Exception e) {
            log.error("Signature validation error: {}", e.getMessage());
            return ValidationCheckResult.failure(
                    "Signature validation failed: " + e.getMessage(),
                    "SIGNATURE_VALIDATION_ERROR",
                    e.getClass().getSimpleName()
            );
        }
    }
    
    /**
     * 자체 서명 인증서 여부 확인
     */
    private boolean isSelfSigned(X509Certificate certificate) {
        return certificate.getIssuerX500Principal().equals(certificate.getSubjectX500Principal());
    }
}

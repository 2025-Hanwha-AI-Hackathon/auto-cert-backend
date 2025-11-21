package com.hwgi.autocert.certificate.validation;

import com.hwgi.autocert.domain.model.ValidationCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 인증서 체인 검증
 * - 인증서 체인이 신뢰할 수 있는 루트 CA까지 유효한지 확인
 */
@Slf4j
@Component
public class ChainValidator implements CertificateValidator {
    
    private final X509TrustManager trustManager;
    
    public ChainValidator() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null); // null로 초기화하면 시스템 기본 truststore 사용
            this.trustManager = (X509TrustManager) tmf.getTrustManagers()[0];
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TrustManager", e);
        }
    }
    
    @Override
    public ValidationCheckResult validate(X509Certificate certificate, List<X509Certificate> certificateChain) {
        try {
            // 체인이 제공되지 않은 경우
            if (certificateChain == null || certificateChain.isEmpty()) {
                log.warn("No certificate chain provided, attempting to validate single certificate");
                return validateSingleCertificate(certificate);
            }
            
            // 체인 검증
            X509Certificate[] chain = certificateChain.toArray(new X509Certificate[0]);
            
            // Let's Encrypt Staging 인증서 여부 확인
            boolean isStagingCert = isLetsEncryptStagingCertificate(certificate);
            
            // TrustManager를 사용한 체인 검증 시도
            try {
                trustManager.checkServerTrusted(chain, "RSA");
                
                // 추가 체인 검증 (PKIX)
                CertPath certPath = validateCertPath(chain);
                
                String environment = isStagingCert ? " (Staging)" : " (Production)";
                String details = String.format(
                        "ChainLength: %d, Environment: %s, RootCA: %s",
                        chain.length,
                        isStagingCert ? "Staging" : "Production",
                        chain[chain.length - 1].getSubjectX500Principal()
                );
                
                return ValidationCheckResult.success(
                        String.format("Certificate chain is valid (%d certificates)%s", chain.length, environment),
                        details
                );
            } catch (CertificateException e) {
                // Staging 인증서인 경우 시스템 Trust Store에 없음
                if (isStagingCert) {
                    log.warn("Let's Encrypt Staging certificate detected - not trusted by browsers");
                    
                    // 체인 구조만 검증하여 구조적으로는 올바른지 확인
                    try {
                        for (int i = 0; i < chain.length - 1; i++) {
                            X509Certificate current = chain[i];
                            X509Certificate issuer = chain[i + 1];
                            current.verify(issuer.getPublicKey());
                        }
                    } catch (Exception structureException) {
                        // 체인 구조도 잘못된 경우
                        return ValidationCheckResult.failure(
                                "Staging certificate chain structure is invalid: " + structureException.getMessage(),
                                "INVALID_STAGING_CHAIN"
                        );
                    }
                    
                    String details = String.format(
                            "ChainLength: %d, Environment: Staging, RootCA: %s",
                            chain.length,
                            chain[chain.length - 1].getSubjectX500Principal()
                    );
                    
                    // Staging 인증서는 실패로 표시 (브라우저에서 신뢰하지 않음)
                    return ValidationCheckResult.failure(
                            "Certificate is from Let's Encrypt STAGING environment and is NOT trusted by browsers (test certificate only)",
                            "UNTRUSTED_STAGING_CERTIFICATE",
                            details
                    );
                }
                throw e;
            }
            
        } catch (CertificateException e) {
            log.error("Certificate chain validation failed: {}", e.getMessage());
            return ValidationCheckResult.failure(
                    "Certificate chain is invalid: " + e.getMessage(),
                    "INVALID_CHAIN",
                    e.getClass().getSimpleName()
            );
        } catch (Exception e) {
            log.error("Chain validation error: {}", e.getMessage());
            return ValidationCheckResult.failure(
                    "Chain validation failed: " + e.getMessage(),
                    "CHAIN_VALIDATION_ERROR",
                    e.getClass().getSimpleName()
            );
        }
    }
    
    /**
     * 단일 인증서 검증 (체인 없이)
     */
    private ValidationCheckResult validateSingleCertificate(X509Certificate certificate) {
        try {
            // 신뢰할 수 있는 CA 목록에 있는지 확인
            X509Certificate[] acceptedIssuers = trustManager.getAcceptedIssuers();
            
            for (X509Certificate issuer : acceptedIssuers) {
                if (issuer.getSubjectX500Principal().equals(certificate.getIssuerX500Principal())) {
                    // 발급자를 찾았으므로 서명 검증
                    certificate.verify(issuer.getPublicKey());
                    
                    return ValidationCheckResult.success(
                            "Certificate is issued by a trusted CA: " + issuer.getSubjectX500Principal(),
                            "TrustedIssuer: " + issuer.getSubjectX500Principal()
                    );
                }
            }
            
            // 신뢰할 수 있는 발급자를 찾지 못함
            log.warn("Certificate issuer not found in system trust store");
            return ValidationCheckResult.success(
                    "Certificate chain not fully verified (issuer not in trust store)",
                    "PartialValidation: Issuer not found in system trust store"
            );
            
        } catch (Exception e) {
            log.error("Single certificate validation error: {}", e.getMessage());
            return ValidationCheckResult.failure(
                    "Certificate validation failed: " + e.getMessage(),
                    "SINGLE_CERT_VALIDATION_ERROR",
                    e.getClass().getSimpleName()
            );
        }
    }
    
    /**
     * PKIX 알고리즘을 사용한 인증서 경로 검증
     */
    private CertPath validateCertPath(X509Certificate[] chain) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        CertPath certPath = cf.generateCertPath(Arrays.asList(chain));
        
        // PKIX 파라미터 설정
        PKIXParameters params = new PKIXParameters(trustManager.getAcceptedIssuers()
                .length > 0 ? createTrustStore() : KeyStore.getInstance(KeyStore.getDefaultType()));
        params.setRevocationEnabled(false); // 폐기 확인은 별도의 Validator에서 수행
        
        // 인증서 경로 검증
        CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        validator.validate(certPath, params);
        
        return certPath;
    }
    
    /**
     * 시스템 신뢰 저장소 생성
     */
    private KeyStore createTrustStore() throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        
        X509Certificate[] acceptedIssuers = trustManager.getAcceptedIssuers();
        for (int i = 0; i < acceptedIssuers.length; i++) {
            trustStore.setCertificateEntry("ca-" + i, acceptedIssuers[i]);
        }
        
        return trustStore;
    }
    
    /**
     * Let's Encrypt Staging 인증서 여부 확인
     */
    private boolean isLetsEncryptStagingCertificate(X509Certificate certificate) {
        String issuer = certificate.getIssuerX500Principal().getName();
        
        // Staging 인증서는 "(STAGING)" 키워드 포함
        // 예: CN=(STAGING) Pretend Pear X1, O=(STAGING) Let's Encrypt, C=US
        return issuer.contains("(STAGING)") || 
               issuer.contains("Fake LE") || 
               issuer.contains("Pretend Pear");
    }
}

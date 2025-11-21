package com.hwgi.autocert.certificate.validation;

import com.hwgi.autocert.domain.model.ValidationCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * 키 사용 검증
 * - Key Usage와 Extended Key Usage가 적절한지 확인
 */
@Slf4j
@Component
public class KeyUsageValidator implements CertificateValidator {
    
    // Key Usage bit positions
    private static final int DIGITAL_SIGNATURE = 0;
    private static final int NON_REPUDIATION = 1;
    private static final int KEY_ENCIPHERMENT = 2;
    private static final int DATA_ENCIPHERMENT = 3;
    private static final int KEY_AGREEMENT = 4;
    private static final int KEY_CERT_SIGN = 5;
    private static final int CRL_SIGN = 6;
    private static final int ENCIPHER_ONLY = 7;
    private static final int DECIPHER_ONLY = 8;
    
    // Extended Key Usage OIDs
    private static final String EKU_SERVER_AUTH = "1.3.6.1.5.5.7.3.1"; // TLS Web Server Authentication
    private static final String EKU_CLIENT_AUTH = "1.3.6.1.5.5.7.3.2"; // TLS Web Client Authentication
    private static final String EKU_CODE_SIGNING = "1.3.6.1.5.5.7.3.3"; // Code Signing
    private static final String EKU_EMAIL_PROTECTION = "1.3.6.1.5.5.7.3.4"; // Email Protection
    
    @Override
    public ValidationCheckResult validate(X509Certificate certificate, List<X509Certificate> certificateChain) {
        try {
            List<String> keyUsages = new ArrayList<>();
            List<String> extendedKeyUsages = new ArrayList<>();
            
            // Key Usage 확인
            boolean[] keyUsageArray = certificate.getKeyUsage();
            if (keyUsageArray != null) {
                keyUsages = extractKeyUsages(keyUsageArray);
            }
            
            // Extended Key Usage 확인
            List<String> ekuOids = certificate.getExtendedKeyUsage();
            if (ekuOids != null) {
                extendedKeyUsages = extractExtendedKeyUsages(ekuOids);
            }
            
            // 서버 인증서로 적절한지 검증
            boolean validForServerAuth = validateServerAuth(keyUsageArray, ekuOids);
            
            String details = String.format(
                    "KeyUsage: %s, ExtendedKeyUsage: %s, ValidForServerAuth: %b",
                    keyUsages.isEmpty() ? "none" : String.join(", ", keyUsages),
                    extendedKeyUsages.isEmpty() ? "none" : String.join(", ", extendedKeyUsages),
                    validForServerAuth
            );
            
            if (!validForServerAuth) {
                log.warn("Certificate may not be suitable for server authentication");
                return ValidationCheckResult.success(
                        "Certificate key usage found but may not be suitable for TLS server authentication",
                        details
                );
            }
            
            return ValidationCheckResult.success(
                    String.format("Certificate has appropriate key usage for TLS server authentication"),
                    details
            );
            
        } catch (Exception e) {
            log.error("Key usage validation error: {}", e.getMessage());
            return ValidationCheckResult.failure(
                    "Key usage validation failed: " + e.getMessage(),
                    "KEY_USAGE_VALIDATION_ERROR",
                    e.getClass().getSimpleName()
            );
        }
    }
    
    /**
     * Key Usage 비트 배열에서 사용 용도 추출
     */
    private List<String> extractKeyUsages(boolean[] keyUsage) {
        List<String> usages = new ArrayList<>();
        
        if (keyUsage.length > DIGITAL_SIGNATURE && keyUsage[DIGITAL_SIGNATURE]) {
            usages.add("digitalSignature");
        }
        if (keyUsage.length > NON_REPUDIATION && keyUsage[NON_REPUDIATION]) {
            usages.add("nonRepudiation");
        }
        if (keyUsage.length > KEY_ENCIPHERMENT && keyUsage[KEY_ENCIPHERMENT]) {
            usages.add("keyEncipherment");
        }
        if (keyUsage.length > DATA_ENCIPHERMENT && keyUsage[DATA_ENCIPHERMENT]) {
            usages.add("dataEncipherment");
        }
        if (keyUsage.length > KEY_AGREEMENT && keyUsage[KEY_AGREEMENT]) {
            usages.add("keyAgreement");
        }
        if (keyUsage.length > KEY_CERT_SIGN && keyUsage[KEY_CERT_SIGN]) {
            usages.add("keyCertSign");
        }
        if (keyUsage.length > CRL_SIGN && keyUsage[CRL_SIGN]) {
            usages.add("cRLSign");
        }
        
        return usages;
    }
    
    /**
     * Extended Key Usage OID에서 사용 용도 추출
     */
    private List<String> extractExtendedKeyUsages(List<String> ekuOids) {
        List<String> usages = new ArrayList<>();
        
        for (String oid : ekuOids) {
            switch (oid) {
                case EKU_SERVER_AUTH:
                    usages.add("serverAuth");
                    break;
                case EKU_CLIENT_AUTH:
                    usages.add("clientAuth");
                    break;
                case EKU_CODE_SIGNING:
                    usages.add("codeSigning");
                    break;
                case EKU_EMAIL_PROTECTION:
                    usages.add("emailProtection");
                    break;
                default:
                    usages.add("other(" + oid + ")");
            }
        }
        
        return usages;
    }
    
    /**
     * 서버 인증에 적합한지 검증
     */
    private boolean validateServerAuth(boolean[] keyUsage, List<String> ekuOids) {
        // Extended Key Usage가 있는 경우, serverAuth가 포함되어야 함
        if (ekuOids != null && !ekuOids.isEmpty()) {
            if (!ekuOids.contains(EKU_SERVER_AUTH)) {
                return false;
            }
        }
        
        // Key Usage가 있는 경우, digitalSignature 또는 keyEncipherment가 있어야 함
        if (keyUsage != null) {
            boolean hasDigitalSignature = keyUsage.length > DIGITAL_SIGNATURE && keyUsage[DIGITAL_SIGNATURE];
            boolean hasKeyEncipherment = keyUsage.length > KEY_ENCIPHERMENT && keyUsage[KEY_ENCIPHERMENT];
            
            if (!hasDigitalSignature && !hasKeyEncipherment) {
                return false;
            }
        }
        
        return true;
    }
}

package com.hwgi.autocert.certificate.validation;

import com.hwgi.autocert.domain.model.Certificate;
import com.hwgi.autocert.domain.model.CertificateValidationResult;
import com.hwgi.autocert.domain.model.ValidationCheckResult;
import com.hwgi.autocert.domain.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 인증서 검증 서비스
 * 모든 검증 항목을 종합하여 인증서의 유효성을 판단
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateValidationService {
    
    private final CertificateRepository certificateRepository;
    private final SignatureValidator signatureValidator;
    private final ValidityPeriodValidator validityPeriodValidator;
    private final ChainValidator chainValidator;
    private final RevocationValidator revocationValidator;
    private final DomainValidator domainValidator;
    private final KeyUsageValidator keyUsageValidator;
    
    /**
     * 데이터베이스에 저장된 인증서 검증
     *
     * @param certificateId 인증서 ID
     * @return 검증 결과
     */
    @Transactional(readOnly = true)
    public CertificateValidationResult validateCertificate(Long certificateId) {
        log.info("Validating certificate with ID: {}", certificateId);
        
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + certificateId));
        
        return validateCertificatePem(certificate.getCertificatePem(), null);
    }
    
    /**
     * PEM 형식의 인증서 검증
     *
     * @param certificatePem PEM 형식의 인증서
     * @param chainPem PEM 형식의 인증서 체인 (선택사항)
     * @return 검증 결과
     */
    public CertificateValidationResult validateCertificatePem(String certificatePem, String chainPem) {
        try {
            // PEM을 X509Certificate로 변환
            X509Certificate x509Cert = parseCertificate(certificatePem);
            List<X509Certificate> chain = null;
            
            if (chainPem != null && !chainPem.trim().isEmpty()) {
                chain = parseCertificateChain(chainPem);
            }
            
            return performValidation(x509Cert, chain);
            
        } catch (Exception e) {
            log.error("Failed to parse certificate: {}", e.getMessage());
            
            CertificateValidationResult result = CertificateValidationResult.builder()
                    .valid(false)
                    .validatedAt(LocalDateTime.now())
                    .errors(new ArrayList<>())
                    .build();
            
            result.addError("Failed to parse certificate: " + e.getMessage());
            
            return result;
        }
    }
    
    /**
     * 모든 검증 수행
     */
    private CertificateValidationResult performValidation(X509Certificate certificate, List<X509Certificate> chain) {
        log.debug("Performing validation on certificate: {}", certificate.getSubjectX500Principal());
        
        // 체인이 없는 경우 단일 인증서로 체인 생성
        if (chain == null) {
            chain = new ArrayList<>();
            chain.add(certificate);
        } else if (!chain.contains(certificate)) {
            // 체인에 인증서가 포함되지 않은 경우 추가
            List<X509Certificate> fullChain = new ArrayList<>();
            fullChain.add(certificate);
            fullChain.addAll(chain);
            chain = fullChain;
        }
        
        // 각 검증 항목 실행
        ValidationCheckResult signatureCheck = signatureValidator.validate(certificate, chain);
        ValidationCheckResult validityCheck = validityPeriodValidator.validate(certificate, chain);
        ValidationCheckResult chainCheck = chainValidator.validate(certificate, chain);
        ValidationCheckResult revocationCheck = revocationValidator.validate(certificate, chain);
        ValidationCheckResult domainCheck = domainValidator.validate(certificate, chain);
        ValidationCheckResult keyUsageCheck = keyUsageValidator.validate(certificate, chain);
        
        // 결과 종합
        CertificateValidationResult result = CertificateValidationResult.builder()
                .validatedAt(LocalDateTime.now())
                .signatureCheck(signatureCheck)
                .validityCheck(validityCheck)
                .chainCheck(chainCheck)
                .revocationCheck(revocationCheck)
                .domainCheck(domainCheck)
                .keyUsageCheck(keyUsageCheck)
                .warnings(new ArrayList<>())
                .errors(new ArrayList<>())
                .build();
        
        // 전체 유효성 판단
        boolean isValid = result.isAllValid();
        result = CertificateValidationResult.builder()
                .valid(isValid)
                .validatedAt(result.getValidatedAt())
                .signatureCheck(result.getSignatureCheck())
                .validityCheck(result.getValidityCheck())
                .chainCheck(result.getChainCheck())
                .revocationCheck(result.getRevocationCheck())
                .domainCheck(result.getDomainCheck())
                .keyUsageCheck(result.getKeyUsageCheck())
                .warnings(result.getWarnings())
                .errors(result.getErrors())
                .build();
        
        // 경고 및 에러 수집
        collectWarningsAndErrors(result, signatureCheck, "Signature");
        collectWarningsAndErrors(result, validityCheck, "Validity");
        collectWarningsAndErrors(result, chainCheck, "Chain");
        collectWarningsAndErrors(result, revocationCheck, "Revocation");
        collectWarningsAndErrors(result, domainCheck, "Domain");
        collectWarningsAndErrors(result, keyUsageCheck, "KeyUsage");
        
        // Staging 인증서 특별 경고 추가
        if (chainCheck != null && !chainCheck.isValid() && 
            chainCheck.getErrorCode() != null && 
            chainCheck.getErrorCode().equals("UNTRUSTED_STAGING_CERTIFICATE")) {
            result.addWarning("⚠️ This is a Let's Encrypt STAGING certificate - NOT trusted by browsers!");
            result.addWarning("Staging certificates should ONLY be used for testing purposes");
            result.addWarning("Deploy a PRODUCTION certificate for real-world use");
        }
        
        log.info("Validation completed. Overall result: {}", isValid ? "VALID" : "INVALID");
        
        return result;
    }
    
    /**
     * 경고 및 에러 메시지 수집
     */
    private void collectWarningsAndErrors(CertificateValidationResult result, 
                                          ValidationCheckResult check, 
                                          String checkName) {
        if (check == null) {
            result.addWarning(checkName + " check was not performed");
            return;
        }
        
        if (!check.isValid()) {
            result.addError(checkName + ": " + check.getMessage());
        } else if (check.getMessage().contains("warning") || 
                   check.getMessage().contains("not fully") ||
                   check.getMessage().contains("soon") ||
                   check.getMessage().contains("Staging") ||
                   check.getMessage().contains("STAGING")) {
            result.addWarning(checkName + ": " + check.getMessage());
        }
    }
    
    /**
     * PEM 문자열을 X509Certificate로 파싱
     */
    private X509Certificate parseCertificate(String pem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream bis = new ByteArrayInputStream(pem.getBytes());
        return (X509Certificate) cf.generateCertificate(bis);
    }
    
    /**
     * PEM 체인 문자열을 X509Certificate 리스트로 파싱
     */
    private List<X509Certificate> parseCertificateChain(String chainPem) throws Exception {
        List<X509Certificate> chain = new ArrayList<>();
        
        // PEM 체인을 개별 인증서로 분리
        String[] certPems = chainPem.split("-----END CERTIFICATE-----");
        
        for (String certPem : certPems) {
            if (certPem.trim().isEmpty()) {
                continue;
            }
            
            String fullPem = certPem.trim() + "\n-----END CERTIFICATE-----";
            X509Certificate cert = parseCertificate(fullPem);
            chain.add(cert);
        }
        
        return chain;
    }
}

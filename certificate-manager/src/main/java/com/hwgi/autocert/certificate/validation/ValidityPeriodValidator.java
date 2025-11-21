package com.hwgi.autocert.certificate.validation;

import com.hwgi.autocert.domain.model.ValidationCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * 인증서 유효기간 검증
 * - notBefore와 notAfter 사이에 현재 시점이 있는지 확인
 */
@Slf4j
@Component
public class ValidityPeriodValidator implements CertificateValidator {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int WARNING_DAYS = 30;
    
    @Override
    public ValidationCheckResult validate(X509Certificate certificate, List<X509Certificate> certificateChain) {
        try {
            Date now = new Date();
            Date notBefore = certificate.getNotBefore();
            Date notAfter = certificate.getNotAfter();
            
            // 유효기간 검증
            certificate.checkValidity(now);
            
            // 만료까지 남은 일수 계산
            long daysUntilExpiry = ChronoUnit.DAYS.between(
                    Instant.now(),
                    notAfter.toInstant()
            );
            
            String details = String.format(
                    "NotBefore: %s, NotAfter: %s, DaysRemaining: %d",
                    formatDate(notBefore),
                    formatDate(notAfter),
                    daysUntilExpiry
            );
            
            // 30일 이내 만료 경고
            if (daysUntilExpiry <= WARNING_DAYS) {
                log.warn("Certificate expires in {} days", daysUntilExpiry);
                return ValidationCheckResult.success(
                        String.format("Certificate is valid but expires soon in %d days", daysUntilExpiry),
                        details
                );
            }
            
            return ValidationCheckResult.success(
                    String.format("Certificate is valid (%d days remaining)", daysUntilExpiry),
                    details
            );
            
        } catch (CertificateExpiredException e) {
            log.error("Certificate has expired: {}", e.getMessage());
            return ValidationCheckResult.failure(
                    "Certificate has expired on " + formatDate(certificate.getNotAfter()),
                    "CERTIFICATE_EXPIRED",
                    "ExpiredAt: " + formatDate(certificate.getNotAfter())
            );
        } catch (CertificateNotYetValidException e) {
            log.error("Certificate is not yet valid: {}", e.getMessage());
            return ValidationCheckResult.failure(
                    "Certificate is not yet valid until " + formatDate(certificate.getNotBefore()),
                    "CERTIFICATE_NOT_YET_VALID",
                    "ValidFrom: " + formatDate(certificate.getNotBefore())
            );
        } catch (Exception e) {
            log.error("Validity period validation error: {}", e.getMessage());
            return ValidationCheckResult.failure(
                    "Validity period validation failed: " + e.getMessage(),
                    "VALIDITY_VALIDATION_ERROR",
                    e.getClass().getSimpleName()
            );
        }
    }
    
    private String formatDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .format(FORMATTER);
    }
}

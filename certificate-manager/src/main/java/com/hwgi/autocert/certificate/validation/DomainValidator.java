package com.hwgi.autocert.certificate.validation;

import com.hwgi.autocert.domain.model.ValidationCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 도메인 일치 검증
 * - Subject Alternative Names (SAN)이 의도한 도메인과 일치하는지 확인
 */
@Slf4j
@Component
public class DomainValidator implements CertificateValidator {
    
    private static final int SAN_DNS_NAME = 2; // DNS Name type in SAN
    
    @Override
    public ValidationCheckResult validate(X509Certificate certificate, List<X509Certificate> certificateChain) {
        try {
            // Subject Alternative Names 추출
            Collection<List<?>> sanCollection = certificate.getSubjectAlternativeNames();
            List<String> dnsNames = new ArrayList<>();
            
            if (sanCollection != null) {
                for (List<?> san : sanCollection) {
                    Integer type = (Integer) san.get(0);
                    if (type == SAN_DNS_NAME) {
                        dnsNames.add((String) san.get(1));
                    }
                }
            }
            
            // SAN이 없는 경우 CN 추출
            if (dnsNames.isEmpty()) {
                String cn = extractCN(certificate.getSubjectX500Principal());
                if (cn != null) {
                    dnsNames.add(cn);
                    log.debug("No SAN found, using CN: {}", cn);
                }
            }
            
            if (dnsNames.isEmpty()) {
                return ValidationCheckResult.failure(
                        "No domain names found in certificate (no SAN or CN)",
                        "NO_DOMAIN_NAMES"
                );
            }
            
            String details = String.format(
                    "Domains: %s, WildcardCount: %d",
                    String.join(", ", dnsNames),
                    dnsNames.stream().filter(d -> d.startsWith("*.")).count()
            );
            
            return ValidationCheckResult.success(
                    String.format("Certificate contains %d domain(s): %s", 
                            dnsNames.size(), 
                            String.join(", ", dnsNames)),
                    details
            );
            
        } catch (Exception e) {
            log.error("Domain validation error: {}", e.getMessage());
            return ValidationCheckResult.failure(
                    "Domain validation failed: " + e.getMessage(),
                    "DOMAIN_VALIDATION_ERROR",
                    e.getClass().getSimpleName()
            );
        }
    }
    
    /**
     * X500Principal에서 CN(Common Name) 추출
     */
    private String extractCN(X500Principal principal) {
        String dn = principal.getName();
        for (String part : dn.split(",")) {
            part = part.trim();
            if (part.toUpperCase().startsWith("CN=")) {
                return part.substring(3);
            }
        }
        return null;
    }
    
    /**
     * 도메인이 인증서에 포함되는지 확인 (와일드카드 지원)
     */
    public boolean matchesDomain(X509Certificate certificate, String domain) {
        try {
            Collection<List<?>> sanCollection = certificate.getSubjectAlternativeNames();
            List<String> certDomains = new ArrayList<>();
            
            if (sanCollection != null) {
                for (List<?> san : sanCollection) {
                    Integer type = (Integer) san.get(0);
                    if (type == SAN_DNS_NAME) {
                        certDomains.add((String) san.get(1));
                    }
                }
            }
            
            // SAN이 없으면 CN 사용
            if (certDomains.isEmpty()) {
                String cn = extractCN(certificate.getSubjectX500Principal());
                if (cn != null) {
                    certDomains.add(cn);
                }
            }
            
            // 도메인 매칭 (와일드카드 포함)
            for (String certDomain : certDomains) {
                if (matchesPattern(domain, certDomain)) {
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Domain matching error: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 와일드카드 패턴 매칭
     * *.example.com은 foo.example.com과 매칭되지만 foo.bar.example.com과는 매칭되지 않음
     */
    private boolean matchesPattern(String domain, String pattern) {
        domain = domain.toLowerCase();
        pattern = pattern.toLowerCase();
        
        // 정확히 일치
        if (domain.equals(pattern)) {
            return true;
        }
        
        // 와일드카드 패턴
        if (pattern.startsWith("*.")) {
            String baseDomain = pattern.substring(2);
            // domain이 *.example.com의 example.com으로 끝나는지 확인
            if (domain.endsWith("." + baseDomain)) {
                // foo.bar.example.com은 매칭 안됨 (서브도메인이 하나여야 함)
                String prefix = domain.substring(0, domain.length() - baseDomain.length() - 1);
                return !prefix.contains(".");
            }
        }
        
        return false;
    }
}

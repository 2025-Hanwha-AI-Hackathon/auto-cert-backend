package com.hwgi.autocert.certificate.acme.dns;

import com.hwgi.autocert.certificate.config.AcmeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * DNS 프로바이더 팩토리
 * 
 * 설정된 프로바이더 타입에 따라 적절한 DnsProvider 구현체를 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DnsProviderFactory {

    private final ApplicationContext applicationContext;
    private final AcmeProperties acmeProperties;

    /**
     * 설정된 DNS 프로바이더 반환
     * 
     * @return DNS 프로바이더 인스턴스
     * @throws IllegalStateException 프로바이더를 찾을 수 없는 경우
     */
    public DnsProvider getDnsProvider() {
        String providerName = acmeProperties.getDns01Provider();
        
        if (providerName == null || providerName.trim().isEmpty()) {
            log.warn("DNS provider not configured, using manual provider as fallback");
            providerName = "manual";
        }
        
        log.info("Loading DNS provider: {}", providerName);
        
        // Spring Context에서 해당 프로바이더 빈 조회
        try {
            DnsProvider provider = findProviderByName(providerName);
            
            if (provider != null) {
                log.info("DNS provider loaded successfully: {} ({})", 
                    provider.getProviderName(), provider.getClass().getSimpleName());
                return provider;
            }
        } catch (Exception e) {
            log.error("Error loading DNS provider: {}", providerName, e);
        }
        
        // Fallback to manual provider
        log.warn("DNS provider '{}' not found, falling back to manual provider", providerName);
        return applicationContext.getBean(ManualDnsProvider.class);
    }

    /**
     * 이름으로 프로바이더 검색
     */
    private DnsProvider findProviderByName(String providerName) {
        // 모든 DnsProvider 빈 조회
        var providers = applicationContext.getBeansOfType(DnsProvider.class);
        
        for (DnsProvider provider : providers.values()) {
            if (providerName.equalsIgnoreCase(provider.getProviderName())) {
                return provider;
            }
        }
        
        return null;
    }
}

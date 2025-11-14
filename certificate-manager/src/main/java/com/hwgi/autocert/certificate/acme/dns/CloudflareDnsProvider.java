package com.hwgi.autocert.certificate.acme.dns;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cloudflare DNS 프로바이더
 * 
 * Cloudflare API를 사용하여 DNS TXT 레코드를 자동으로 관리
 * 
 * 필수 환경변수:
 * - CLOUDFLARE_API_TOKEN: Cloudflare API 토큰 (Zone.DNS 권한 필요)
 * 또는
 * - CLOUDFLARE_EMAIL: Cloudflare 계정 이메일
 * - CLOUDFLARE_API_KEY: Cloudflare Global API Key
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "autocert.certificate.acme.dns01-provider", havingValue = "cloudflare")
public class CloudflareDnsProvider implements DnsProvider {

    private static final String PROVIDER_NAME = "cloudflare";
    private static final String CLOUDFLARE_API_URL = "https://api.cloudflare.com/client/v4";
    private static final int DNS_PROPAGATION_CHECK_INTERVAL = 10; // 10초
    private static final int DNS_PROPAGATION_INITIAL_DELAY = 20; // 최소 대기 시간 20초
    
    private final RestTemplate restTemplate;
    private final String apiToken;

    public CloudflareDnsProvider(@Value("${cloudflare.api-token}") String apiToken) {
        this.restTemplate = new RestTemplate();
        this.apiToken = apiToken;

        validateCredentials();
    }

    /**
     * 자격증명 검증
     */
    private void validateCredentials() {
        if (apiToken == null) {
            throw new IllegalStateException(
                "Cloudflare credentials not configured. " +
                "Please set either CLOUDFLARE_API_TOKEN or both CLOUDFLARE_EMAIL and CLOUDFLARE_API_KEY"
            );
        }
    }

    @Override
    public void addTxtRecord(String domain, String recordName, String recordValue) throws Exception {
        log.info("Adding DNS TXT record to Cloudflare: {}.{} = {}", recordName, domain, recordValue);
        
        String zoneId = getZoneId(domain);
        String fullRecordName = recordName + "." + domain;
        
        // TXT 레코드 생성 요청
        Map<String, Object> recordData = new HashMap<>();
        recordData.put("type", "TXT");
        recordData.put("name", fullRecordName);
        recordData.put("content", recordValue);
        recordData.put("ttl", 120); // 2분 (최소값)
        
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(recordData, headers);
        
        String url = CLOUDFLARE_API_URL + "/zones/" + zoneId + "/dns_records";
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && 
                Boolean.TRUE.equals(response.getBody().get("success"))) {
                log.info("DNS TXT record added successfully to Cloudflare");
            } else {
                throw new Exception("Failed to add DNS record: " + response.getBody());
            }
        } catch (HttpClientErrorException e) {
            log.error("Cloudflare API error: {}", e.getResponseBodyAsString());
            throw new Exception("Failed to add DNS TXT record to Cloudflare", e);
        }
    }

    @Override
    public void removeTxtRecord(String domain, String recordName, String recordValue) throws Exception {
        log.info("Removing DNS TXT record from Cloudflare: {}.{}", recordName, domain);
        
        try {
            String zoneId = getZoneId(domain);
            String recordId = findRecordId(zoneId, recordName + "." + domain, recordValue);
            
            if (recordId != null) {
                HttpHeaders headers = createHeaders();
                HttpEntity<Void> request = new HttpEntity<>(headers);
                
                String url = CLOUDFLARE_API_URL + "/zones/" + zoneId + "/dns_records/" + recordId;
                
                restTemplate.exchange(url, HttpMethod.DELETE, request, Map.class);
                log.info("DNS TXT record removed successfully from Cloudflare");
            } else {
                log.warn("DNS TXT record not found for removal: {}.{}", recordName, domain);
            }
        } catch (Exception e) {
            log.warn("Failed to remove DNS TXT record (non-critical): {}", e.getMessage());
        }
    }

    @Override
    public boolean waitForPropagation(String domain, String recordName, String recordValue, int timeoutSeconds) {
        log.info("Waiting for DNS propagation... (max {} seconds)", timeoutSeconds);
        
        String fullRecordName = recordName + "." + domain;
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;
        
        // Cloudflare DNS 전파를 위한 최소 대기 시간
        try {
            log.info("Initial delay for Cloudflare DNS propagation: {} seconds", DNS_PROPAGATION_INITIAL_DELAY);
            TimeUnit.SECONDS.sleep(DNS_PROPAGATION_INITIAL_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Initial delay interrupted");
            return false;
        }
        
        int attempt = 0;
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            attempt++;
            
            try {
                if (verifyDnsRecord(domain, fullRecordName, recordValue)) {
                    log.info("DNS propagation verified after {} attempts", attempt);
                    return true;
                }
                
                log.debug("DNS record not yet propagated, waiting... (attempt {})", attempt);
                TimeUnit.SECONDS.sleep(DNS_PROPAGATION_CHECK_INTERVAL);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("DNS propagation check interrupted");
                return false;
            } catch (Exception e) {
                log.warn("Error checking DNS propagation: {}", e.getMessage());
            }
        }
        
        log.warn("DNS propagation timeout after {} attempts", attempt);
        return false;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * Zone ID 조회
     */
    private String getZoneId(String domain) throws Exception {
        // 도메인에서 Zone 추출 (예: test.example.com -> example.com)
        String zoneName = extractZoneName(domain);
        
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        String url = CLOUDFLARE_API_URL + "/zones?name=" + zoneName;
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                request, 
                Map.class
            );
            
            List<Map<String, Object>> result = (List<Map<String, Object>>) response.getBody().get("result");
            
            if (result == null || result.isEmpty()) {
                throw new Exception("Zone not found for domain: " + zoneName);
            }
            
            return (String) result.get(0).get("id");
            
        } catch (Exception e) {
            log.error("Failed to get zone ID for domain: {}", zoneName, e);
            throw new Exception("Failed to get Cloudflare zone ID", e);
        }
    }

    /**
     * DNS 레코드 ID 조회
     */
    private String findRecordId(String zoneId, String recordName, String recordValue) throws Exception {
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        String url = CLOUDFLARE_API_URL + "/zones/" + zoneId + "/dns_records?type=TXT&name=" + recordName;
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                request, 
                Map.class
            );
            
            List<Map<String, Object>> result = (List<Map<String, Object>>) response.getBody().get("result");
            
            if (result != null) {
                for (Map<String, Object> record : result) {
                    if (recordValue.equals(record.get("content"))) {
                        return (String) record.get("id");
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Failed to find DNS record ID", e);
            throw new Exception("Failed to find DNS record ID", e);
        }
    }

    /**
     * DNS 레코드 검증 - 실제 공개 DNS 서버에서 확인
     * 
     * Cloudflare API가 아닌 실제 DNS 쿼리로 전파 여부 확인
     */
    private boolean verifyDnsRecord(String domain, String recordName, String expectedValue) throws Exception {
        try {
            // DNS TXT 레코드 조회
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            // Google Public DNS 사용 (신뢰성 높음)
            env.put(Context.PROVIDER_URL, "dns://8.8.8.8");
            
            DirContext context = new InitialDirContext(env);
            Attributes attrs = context.getAttributes(recordName, new String[]{"TXT"});
            Attribute txtAttr = attrs.get("TXT");
            
            if (txtAttr != null) {
                for (int i = 0; i < txtAttr.size(); i++) {
                    String value = (String) txtAttr.get(i);
                    // TXT 레코드는 따옴표로 감싸져 있을 수 있음
                    String cleanValue = value.replaceAll("^\"|\"$", "");
                    
                    log.debug("Found DNS TXT record: {} = {}", recordName, cleanValue);
                    
                    if (expectedValue.equals(cleanValue)) {
                        log.info("DNS TXT record verified on public DNS: {}", recordName);
                        return true;
                    }
                }
            }
            
            log.debug("DNS TXT record not found or value mismatch: {}", recordName);
            return false;
            
        } catch (NamingException e) {
            log.debug("DNS lookup failed for {}: {}", recordName, e.getMessage());
            return false;
        }
    }

    /**
     * HTTP 헤더 생성
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiToken);
        
        return headers;
    }

    /**
     * 도메인에서 Zone 이름 추출
     * 
     * 예:
     * - example.com -> example.com
     * - test.example.com -> example.com
     * - sub.test.example.com -> example.com
     */
    private String extractZoneName(String domain) {
        String[] parts = domain.split("\\.");
        
        if (parts.length <= 2) {
            return domain;
        }
        
        // 마지막 2개 부분을 Zone으로 간주 (example.com)
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }
}

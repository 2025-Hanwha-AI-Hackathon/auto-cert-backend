package com.hwgi.autocert.certificate.acme.challenge;

import com.hwgi.autocert.certificate.acme.dns.DnsProvider;
import com.hwgi.autocert.certificate.acme.dns.DnsProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * DNS-01 챌린지 핸들러
 * 
 * DNS TXT 레코드를 추가하여 도메인 소유권 증명
 * 
 * 장점:
 * - 와일드카드 인증서 지원 (*.example.com)
 * - 내부 서버도 가능 (외부 접근 불필요)
 * 
 * 단점:
 * - DNS API 연동 필요
 * - DNS 전파 시간 소요
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Dns01ChallengeHandler implements ChallengeHandler {

    private static final String ACME_CHALLENGE_PREFIX = "_acme-challenge";
    private static final int DNS_PROPAGATION_TIMEOUT = 300; // 5분

    private final DnsProviderFactory dnsProviderFactory;

    /**
     * DNS-01 챌린지 준비
     * 
     * DNS TXT 레코드 추가: _acme-challenge.example.com
     */
    @Override
    public void prepare(String domain, Challenge challenge) throws Exception {
        if (!(challenge instanceof Dns01Challenge)) {
            throw new IllegalArgumentException("Challenge must be Dns01Challenge");
        }

        Dns01Challenge dns01 = (Dns01Challenge) challenge;
        String digest = dns01.getDigest();
        
        log.info("Preparing DNS-01 challenge for domain: {}", domain);
        log.debug("Digest: {}", digest);

        // DNS TXT 레코드 추가
        DnsProvider dnsProvider = dnsProviderFactory.getDnsProvider();
        dnsProvider.addTxtRecord(domain, ACME_CHALLENGE_PREFIX, digest);
        
        log.info("DNS TXT record added: {}.{} = {}", ACME_CHALLENGE_PREFIX, domain, digest);

        // DNS 전파 대기
        log.info("Waiting for DNS propagation...");
        boolean propagated = dnsProvider.waitForPropagation(
            domain, 
            ACME_CHALLENGE_PREFIX, 
            digest, 
            DNS_PROPAGATION_TIMEOUT
        );

        if (!propagated) {
            log.warn("DNS propagation timeout, but continuing with validation");
        } else {
            log.info("DNS propagation completed");
        }
    }

    /**
     * DNS-01 챌린지 검증
     * 
     * ACME 서버가 DNS TXT 레코드를 조회하여 검증
     */
    @Override
    public void validate(Challenge challenge) throws Exception {
        if (!(challenge instanceof Dns01Challenge)) {
            throw new IllegalArgumentException("Challenge must be Dns01Challenge");
        }

        log.info("Triggering DNS-01 challenge validation");
        
        // 챌린지 트리거
        challenge.trigger();
        
        // 챌린지 상태 폴링 (최대 5분)
        int attempts = 0;
        int maxAttempts = 100; // 5분 (3초 간격)
        
        while (challenge.getStatus() != org.shredzone.acme4j.Status.VALID && attempts < maxAttempts) {
            if (challenge.getStatus() == org.shredzone.acme4j.Status.INVALID) {
                throw new AcmeException("DNS-01 challenge validation failed: " + 
                    challenge.getError().map(error -> error.toString()).orElse("Unknown error"));
            }
            
            // 3초 대기
            TimeUnit.SECONDS.sleep(3);
            
            // 상태 업데이트
            challenge.update();
            attempts++;
            
            log.debug("Challenge status: {} (attempt {}/{})", 
                challenge.getStatus(), attempts, maxAttempts);
        }
        
        if (challenge.getStatus() != org.shredzone.acme4j.Status.VALID) {
            throw new AcmeException("DNS-01 challenge validation timeout");
        }
        
        log.info("DNS-01 challenge validated successfully");
    }

    /**
     * DNS-01 챌린지 정리
     * 
     * DNS TXT 레코드 삭제
     */
    @Override
    public void cleanup(String domain, Challenge challenge) {
        if (!(challenge instanceof Dns01Challenge)) {
            log.warn("Challenge is not Dns01Challenge, skipping cleanup");
            return;
        }

        Dns01Challenge dns01 = (Dns01Challenge) challenge;
        String digest = dns01.getDigest();
        
        try {
            DnsProvider dnsProvider = dnsProviderFactory.getDnsProvider();
            dnsProvider.removeTxtRecord(domain, ACME_CHALLENGE_PREFIX, digest);
            log.info("DNS TXT record removed: {}.{}", ACME_CHALLENGE_PREFIX, domain);
        } catch (Exception e) {
            log.warn("Failed to remove DNS TXT record for domain: {}", domain, e);
        }
    }

    @Override
    public ChallengeType getChallengeType() {
        return ChallengeType.DNS_01;
    }
}

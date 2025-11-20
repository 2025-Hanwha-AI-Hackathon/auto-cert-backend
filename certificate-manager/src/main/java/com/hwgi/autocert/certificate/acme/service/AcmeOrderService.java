package com.hwgi.autocert.certificate.acme.service;

import com.hwgi.autocert.certificate.acme.challenge.ChallengeHandler;
import com.hwgi.autocert.certificate.acme.challenge.ChallengeHandlerFactory;
import com.hwgi.autocert.certificate.acme.challenge.ChallengeType;
import com.hwgi.autocert.certificate.config.AcmeProperties;
import com.hwgi.autocert.certificate.service.AcmeAccountService;
import com.hwgi.autocert.domain.model.AcmeAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.util.concurrent.TimeUnit;

/**
 * ACME 주문 서비스
 * 
 * ACME 프로토콜을 통한 인증서 발급 및 갱신 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcmeOrderService {

    private final AcmeAccountService acmeAccountService;
    private final ChallengeHandlerFactory challengeHandlerFactory;
    private final AcmeProperties acmeProperties;

    /**
     * 인증서 발급 (기본 챌린지 타입 사용)
     *
     * @param domain 도메인명
     * @return 발급된 인증서 및 개인키
     */
    public CertificateResult issueCertificate(String domain) throws Exception {
        return issueCertificate(domain, acmeProperties.getDefaultChallengeType());
    }

    /**
     * 인증서 발급
     *
     * @param domain 도메인명
     * @param challengeType 챌린지 타입 (HTTP_01, DNS_01)
     * @return 발급된 인증서 및 개인키
     */
    public CertificateResult issueCertificate(String domain, ChallengeType challengeType) throws Exception {
        log.info("Starting certificate issuance for domain: {}, challengeType: {}", domain, challengeType);

        // 1. ACME 계정 조회/생성
        AcmeAccount acmeAccount = acmeAccountService.getOrCreateDefaultAccount();
        Account account = acmeAccountService.getAcmeAccount(acmeAccount);
        
        // 2. 도메인 키페어 생성 (인증서용)
        // 매번 새로운 키페어 생성으로 항상 새로운 인증서 발급
        KeyPair domainKeyPair = KeyPairUtils.createKeyPair(2048);
        log.info("New domain key pair generated for fresh certificate issuance");

        // 3. 주문 생성
        Order order = account.newOrder()
                .domains(domain)
                .create();
        log.info("Order created: {}", order.getLocation());

        // 4. Authorization 처리 (챌린지)
        for (Authorization auth : order.getAuthorizations()) {
            processAuthorization(auth, domain, challengeType);
        }

        // 5. CSR 생성 및 제출
        CSRBuilder csrBuilder = new CSRBuilder();
        csrBuilder.addDomain(domain);
        csrBuilder.sign(domainKeyPair);
        
        byte[] csr = csrBuilder.getEncoded();
        order.execute(csr);
        log.info("CSR submitted to ACME server");

        // 6. 주문 완료 대기
        waitForOrderCompletion(order);

        // 7. 인증서 다운로드
        Certificate certificate = order.getCertificate();
        if (certificate == null) {
            throw new AcmeException("Failed to obtain certificate");
        }

        String certificatePem = convertCertificateToPem(certificate);
        String privateKeyPem = convertKeyPairToPem(domainKeyPair);
        String chainPem = convertCertificateChainToPem(certificate);

        log.info("Certificate issued successfully for domain: {}", domain);

        return CertificateResult.builder()
                .certificatePem(certificatePem)
                .privateKeyPem(privateKeyPem)
                .chainPem(chainPem)
                .domain(domain)
                .build();
    }

    /**
     * 인증서 발급 (문자열 챌린지 타입 사용 - 하위 호환성)
     *
     * @param domain 도메인명
     * @param challengeType 챌린지 타입 문자열 (http-01, dns-01)
     * @return 발급된 인증서 및 개인키
     * @deprecated ChallengeType enum을 사용하는 메서드를 권장합니다
     */
    @Deprecated
    public CertificateResult issueCertificate(String domain, String challengeType) throws Exception {
        ChallengeType type = ChallengeType.fromValue(challengeType);
        return issueCertificate(domain, type);
    }

    /**
     * Authorization 처리 (챌린지 수행)
     */
    private void processAuthorization(Authorization auth, String domain, ChallengeType challengeType) throws Exception {
        log.info("Processing authorization for domain: {}", auth.getIdentifier().getDomain());

        // 챌린지 선택
        Challenge challenge = selectChallenge(auth, challengeType);
        if (challenge == null) {
            throw new AcmeException("No suitable challenge found for type: " + challengeType);
        }

        log.info("Selected challenge type: {}", challenge.getType());

        // 챌린지 핸들러 조회
        ChallengeHandler handler = challengeHandlerFactory.getHandler(challengeType);

        try {
            // 1. 챌린지 준비
            handler.prepare(domain, challenge);

            // 2. 챌린지 검증
            handler.validate(challenge);

            log.info("Authorization completed for domain: {}", domain);

        } finally {
            // 3. 챌린지 정리
            handler.cleanup(domain, challenge);
        }
    }

    /**
     * 챌린지 선택
     */
    private Challenge selectChallenge(Authorization auth, ChallengeType challengeType) {
        return auth.findChallenge(challengeType.getValue()).orElse(null);
    }

    /**
     * 주문 완료 대기
     */
    private void waitForOrderCompletion(Order order) throws AcmeException, InterruptedException {
        log.info("Waiting for order completion...");

        int attempts = 0;
        int maxAttempts = acmeProperties.getOrderTimeout() / 5; // 5초 간격
        
        while (order.getStatus() != Status.VALID && attempts < maxAttempts) {
            if (order.getStatus() == Status.INVALID) {
                throw new AcmeException("Order failed: " + 
                    order.getError().map(error -> error.toString()).orElse("Unknown error"));
            }

            // 5초 대기
            TimeUnit.SECONDS.sleep(5);

            // 상태 업데이트
            order.update();
            attempts++;

            log.debug("Order status: {} (attempt {}/{})", 
                order.getStatus(), attempts, maxAttempts);
        }

        if (order.getStatus() != Status.VALID) {
            throw new AcmeException("Order completion timeout");
        }

        log.info("Order completed successfully");
    }

    /**
     * 인증서를 PEM 형식으로 변환
     */
    private String convertCertificateToPem(Certificate certificate) throws IOException {
        StringWriter writer = new StringWriter();
        certificate.writeCertificate(writer);
        return writer.toString();
    }

    /**
     * 키페어를 PEM 형식으로 변환
     */
    private String convertKeyPairToPem(KeyPair keyPair) throws IOException {
        StringWriter writer = new StringWriter();
        KeyPairUtils.writeKeyPair(keyPair, writer);
        return writer.toString();
    }

    /**
     * 인증서 체인을 PEM 형식으로 변환
     */
    private String convertCertificateChainToPem(Certificate certificate) throws IOException {
        // 전체 인증서 체인 가져오기
        StringWriter writer = new StringWriter();
        certificate.writeCertificate(writer);
        return writer.toString();
    }

    /**
     * 인증서 발급 결과
     */
    @lombok.Builder
    @lombok.Getter
    public static class CertificateResult {
        private String domain;
        private String certificatePem;
        private String privateKeyPem;
        private String chainPem;
    }
}

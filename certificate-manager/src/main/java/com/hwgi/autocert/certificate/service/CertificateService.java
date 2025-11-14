package com.hwgi.autocert.certificate.service;

import com.hwgi.autocert.certificate.acme.challenge.ChallengeType;
import com.hwgi.autocert.certificate.acme.service.AcmeOrderService;
import com.hwgi.autocert.certificate.config.AcmeProperties;
import com.hwgi.autocert.certificate.util.CertificateEncryptionUtil;
import com.hwgi.autocert.common.exception.ResourceNotFoundException;
import com.hwgi.autocert.domain.model.Certificate;
import com.hwgi.autocert.domain.model.CertificateStatus;
import com.hwgi.autocert.domain.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.hwgi.autocert.domain.model.CertificateStatus.*;

/**
 * 인증서 관리 서비스
 * MVP: 기본 CRUD 기능 제공
 * Phase 2: ACME 프로토콜 통합 및 자동 갱신
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final AcmeOrderService acmeOrderService;
    private final CertificateEncryptionUtil encryptionUtil;
    private final AcmeProperties acmeProperties;

    /**
     * 모든 인증서 조회 (페이지네이션)
     */
    public Page<Certificate> findAll(Pageable pageable) {
        log.debug("Finding all certificates - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return certificateRepository.findAll(pageable);
    }

    /**
     * ID로 인증서 조회
     */
    public Certificate findById(Long id) {
        log.debug("Finding certificate by id: {}", id);
        return certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("인증서를 찾을 수 없습니다: " + id));
    }

    /**
     * 도메인으로 인증서 조회
     */
    public Certificate findByDomain(String domain) {
        log.debug("Finding certificate by domain: {}", domain);
        return certificateRepository.findByDomain(domain)
                .orElseThrow(() -> new ResourceNotFoundException("도메인의 인증서를 찾을 수 없습니다: " + domain));
    }

    /**
     * 상태별 인증서 조회
     */
    public Page<Certificate> findByStatus(CertificateStatus status, Pageable pageable) {
        log.debug("Finding certificates by status: {}", status);
        return certificateRepository.findByStatus(status, pageable);
    }

    /**
     * 도메인 패턴으로 검색
     */
    public Page<Certificate> searchByDomain(String pattern, Pageable pageable) {
        log.debug("Searching certificates by domain pattern: {}", pattern);
        return certificateRepository.searchByDomainPattern(pattern, pageable);
    }

    /**
     * 인증서 생성 (ACME 프로토콜 통합)
     *
     * @param domain 도메인명
     * @param certificateType 인증서 타입 (미사용 - 향후 확장용)
     * @param challengeType 챌린지 타입 (http-01, dns-01)
     * @return 생성된 인증서
     */
    @Transactional
    public Certificate create(String domain, String challengeType) {
        log.info("Creating certificate for domain: {} with challengeType: {}", domain, challengeType);

        // 중복 확인
        if (certificateRepository.existsByDomain(domain)) {
            throw new IllegalArgumentException("이미 존재하는 도메인입니다: " + domain);
        }

        // 챌린지 타입 기본값 설정
        ChallengeType actualChallengeType = StringUtils.hasLength(challengeType)
            ? ChallengeType.fromValue(challengeType)
            : acmeProperties.getDefaultChallengeType();

        try {
            // 1. 상태를 PENDING으로 DB에 먼저 저장
            Certificate certificate = Certificate.builder()
                    .domain(domain)
                    .status(PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            certificate = certificateRepository.save(certificate);
            log.info("Certificate record created with id: {}", certificate.getId());

            // 2. ACME를 통한 실제 인증서 발급
            log.info("Starting ACME certificate issuance for domain: {}", domain);
            AcmeOrderService.CertificateResult result = acmeOrderService.issueCertificate(
                domain,
                actualChallengeType
            );

            // 3. 인증서 정보 파싱
            X509Certificate x509Cert = parseCertificate(result.getCertificatePem());
            LocalDateTime issuedAt = x509Cert.getNotBefore()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            LocalDateTime expiresAt = x509Cert.getNotAfter()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

            // 4. 개인키 암호화
            String encryptedPrivateKey = encryptionUtil.encrypt(result.getPrivateKeyPem());

            // 5. 인증서 업데이트
            certificate.setCertificatePem(result.getCertificatePem());
            certificate.setPrivateKeyPem(encryptedPrivateKey);
            certificate.setChainPem(result.getChainPem());
            certificate.setIssuedAt(issuedAt);
            certificate.setExpiresAt(expiresAt);
            certificate.setStatus(ACTIVE);
            certificate.setUpdatedAt(LocalDateTime.now());

            Certificate saved = certificateRepository.save(certificate);
            log.info("Certificate issued and saved successfully for domain: {}, expires at: {}",
                domain, expiresAt);

            return saved;

        } catch (Exception e) {
            log.error("Failed to create certificate for domain: {}", domain, e);

            // 실패 시 상태 업데이트 (이미 DB에 저장된 경우)
            certificateRepository.findByDomain(domain).ifPresent(cert -> {
                cert.setStatus(FAILED);
                cert.setUpdatedAt(LocalDateTime.now());
                certificateRepository.save(cert);
            });

            throw new RuntimeException("인증서 발급 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 인증서 갱신 (ACME 프로토콜 통합)
     *
     * @param id 인증서 ID
     * @return 갱신된 인증서
     */
    @Transactional
    public Certificate renew(Long id) {
        log.info("Renewing certificate: {}", id);

        Certificate certificate = findById(id);
        String domain = certificate.getDomain();

        try {
            // 1. 상태를 RENEWING으로 업데이트
            certificate.setStatus(RENEWING);
            certificate.setUpdatedAt(LocalDateTime.now());
            certificateRepository.save(certificate);

            // 2. ACME를 통한 인증서 재발급 (기본 챌린지 타입 사용)
            log.info("Starting ACME certificate renewal for domain: {}", domain);
            AcmeOrderService.CertificateResult result = acmeOrderService.issueCertificate(
                domain,
                acmeProperties.getDefaultChallengeType()
            );

            // 3. 인증서 정보 파싱
            X509Certificate x509Cert = parseCertificate(result.getCertificatePem());
            LocalDateTime issuedAt = x509Cert.getNotBefore()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            LocalDateTime expiresAt = x509Cert.getNotAfter()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

            // 4. 개인키 암호화
            String encryptedPrivateKey = encryptionUtil.encrypt(result.getPrivateKeyPem());

            // 5. 인증서 업데이트
            certificate.setCertificatePem(result.getCertificatePem());
            certificate.setPrivateKeyPem(encryptedPrivateKey);
            certificate.setChainPem(result.getChainPem());
            certificate.setIssuedAt(issuedAt);
            certificate.setExpiresAt(expiresAt);
            certificate.setStatus(ACTIVE);
            certificate.setUpdatedAt(LocalDateTime.now());

            Certificate renewed = certificateRepository.save(certificate);
            log.info("Certificate renewed successfully for domain: {}, new expiry: {}",
                domain, expiresAt);

            return renewed;

        } catch (Exception e) {
            log.error("Failed to renew certificate for domain: {}", domain, e);

            // 실패 시 상태 업데이트
            certificate.setStatus(FAILED);
            certificate.setUpdatedAt(LocalDateTime.now());
            certificateRepository.save(certificate);

            throw new RuntimeException("인증서 갱신 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 인증서 삭제
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting certificate: {}", id);

        Certificate certificate = findById(id);
        certificateRepository.delete(certificate);

        log.info("Certificate deleted: {}", id);
    }

    /**
     * 만료 임박 인증서 조회 (갱신 대상)
     */
    public List<Certificate> findExpiringCertificates(int daysBeforeExpiry) {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(daysBeforeExpiry);
        log.debug("Finding certificates expiring before: {}", expiryDate);

        return certificateRepository.findCertificatesExpiringBefore(expiryDate);
    }

    /**
     * 개인키 복호화
     *
     * @param certificate 인증서
     * @return 복호화된 개인키 PEM
     */
    public String decryptPrivateKey(Certificate certificate) {
        String privateKeyPem = certificate.getPrivateKeyPem();

        if (privateKeyPem == null || privateKeyPem.isEmpty()) {
            throw new IllegalStateException("개인키가 없습니다");
        }

        // 이미 복호화된 상태인지 확인
        if (!encryptionUtil.isEncrypted(privateKeyPem)) {
            log.debug("Private key is already decrypted");
            return privateKeyPem;
        }

        // 복호화
        return encryptionUtil.decrypt(privateKeyPem);
    }

    /**
     * PEM 문자열에서 X509Certificate 객체 파싱
     */
    private X509Certificate parseCertificate(String certificatePem) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(certificatePem.getBytes());
        return (X509Certificate) factory.generateCertificate(inputStream);
    }

}

package com.hwgi.autocert.certificate.service;

import com.hwgi.autocert.certificate.acme.challenge.ChallengeType;
import com.hwgi.autocert.certificate.acme.service.AcmeOrderService;
import com.hwgi.autocert.certificate.config.AcmeProperties;
import com.hwgi.autocert.certificate.util.CertificateEncryptionUtil;
import com.hwgi.autocert.certificate.distribution.service.CertificateDistributionService;
import com.hwgi.autocert.common.exception.ResourceNotFoundException;
import com.hwgi.autocert.domain.model.Certificate;
import com.hwgi.autocert.domain.model.CertificateStatus;
import com.hwgi.autocert.domain.repository.CertificateRepository;
import com.hwgi.autocert.domain.repository.ServerRepository;
import com.hwgi.autocert.domain.repository.DeploymentRepository;
import com.hwgi.autocert.domain.model.Server;
import com.hwgi.autocert.domain.model.Deployment;
import com.hwgi.autocert.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final ServerRepository serverRepository;
    private final DeploymentRepository deploymentRepository;
    private final AcmeOrderService acmeOrderService;
    private final CertificateEncryptionUtil encryptionUtil;
    private final AcmeProperties acmeProperties;
    private final CertificateDistributionService distributionService;

    @Autowired(required = false)
    private java.util.Optional<EmailService> emailService;

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
     * @param serverId 서버 ID
     * @param domain 도메인명
     * @param challengeType 챌린지 타입 (http-01, dns-01)
     * @param admin 관리자 또는 담당자
     * @param alertDaysBeforeExpiry 만료 전 알림 일수
     * @param autoDeploy 서버에 자동 배포 여부
     * @return 생성된 인증서
     */
    @Transactional
    public Certificate create(Long serverId, String domain, String challengeType, String admin, Integer alertDaysBeforeExpiry, Boolean autoDeploy) {
        log.info("Creating certificate for domain: {} with challengeType: {}, serverId: {}", domain, challengeType, serverId);

        // 서버 조회
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("서버를 찾을 수 없습니다: " + serverId));

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
                    .server(server)
                    .domain(domain)
                    .status(PENDING)
                    .admin(admin)
                    .alertDaysBeforeExpiry(alertDaysBeforeExpiry != null ? alertDaysBeforeExpiry : 7)
                    .autoDeploy(autoDeploy != null ? autoDeploy : false)
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
            
            log.info("NEW certificate issued - Domain: {}, Serial: {}, IssuedAt: {}, ExpiresAt: {}", 
                domain, x509Cert.getSerialNumber(), issuedAt, expiresAt);

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

            // 6. 자동 배포 처리 (저장된 autoDeploy 설정 사용)
            if (Boolean.TRUE.equals(saved.getAutoDeploy())) {
                log.info("Auto-deployment enabled for certificate: {}", saved.getId());
                deployToServer(saved);
            }

            // 7. 이메일 알림 발송
            emailService.ifPresent(service -> service.sendCertificateCreated(saved));

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
     * @param autoDeploy 서버에 자동 배포 여부 (null이면 저장된 설정 사용)
     * @return 갱신된 인증서
     */
    @Transactional
    public Certificate renew(Long id, Boolean autoDeploy) {
        log.info("Renewing certificate: {}", id);

        Certificate certificate = findById(id);
        String domain = certificate.getDomain();
        
        // autoDeploy 결정: 파라미터가 있으면 우선 사용, 없으면 저장된 설정 사용
        boolean shouldAutoDeploy = autoDeploy != null
            ? autoDeploy
            : Boolean.TRUE.equals(certificate.getAutoDeploy());

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
            
            log.info("RENEWED certificate details - Domain: {}, Serial: {}, IssuedAt: {}, ExpiresAt: {}", 
                domain, x509Cert.getSerialNumber(), issuedAt, expiresAt);

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

            // 6. 자동 배포 처리 (결정된 autoDeploy 설정 사용)
            if (shouldAutoDeploy) {
                log.info("Auto-deployment enabled for renewed certificate: {} (override: {}, stored: {})", 
                    renewed.getId(), autoDeploy, renewed.getAutoDeploy());
                deployToServer(renewed);
            }

            // 7. 이메일 알림 발송
            emailService.ifPresent(service -> service.sendCertificateRenewed(renewed));

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
     * 인증서 정보 수정
     * 
     * @param id 인증서 ID
     * @param domain 도메인 (nullable)
     * @param serverId 서버 ID (nullable)
     * @param issuedAt 발급 일시 (nullable)
     * @param expiresAt 만료 일시 (nullable)
     * @param status 상태 (nullable)
     * @param certificatePem 인증서 PEM (nullable)
     * @param privateKeyPem 개인키 PEM (nullable)
     * @param chainPem 체인 PEM (nullable)
     * @param password 비밀번호 (nullable)
     * @param admin 관리자 (nullable)
     * @param alertDaysBeforeExpiry 알림 일수 (nullable)
     * @param autoDeploy 자동 배포 여부 (nullable)
     * @return 수정된 인증서
     */
    @Transactional
    public Certificate update(
            Long id,
            String domain,
            Long serverId,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt,
            CertificateStatus status,
            String certificatePem,
            String privateKeyPem,
            String chainPem,
            String password,
            String admin,
            Integer alertDaysBeforeExpiry,
            Boolean autoDeploy) {
        log.info("Updating certificate: {}", id);
        
        Certificate certificate = findById(id);
        
        // 도메인 수정
        if (domain != null) {
            certificate.setDomain(domain);
        }
        
        // 서버 수정
        if (serverId != null) {
            Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("서버를 찾을 수 없습니다: " + serverId));
            certificate.setServer(server);
        }
        
        // 발급 일시 수정
        if (issuedAt != null) {
            certificate.setIssuedAt(issuedAt);
        }
        
        // 만료 일시 수정
        if (expiresAt != null) {
            certificate.setExpiresAt(expiresAt);
        }
        
        // 상태 수정
        if (status != null) {
            certificate.setStatus(status);
        }
        
        // 인증서 PEM 수정
        if (certificatePem != null) {
            certificate.setCertificatePem(certificatePem);
        }
        
        // 개인키 PEM 수정
        if (privateKeyPem != null) {
            certificate.setPrivateKeyPem(privateKeyPem);
        }
        
        // 체인 PEM 수정
        if (chainPem != null) {
            certificate.setChainPem(chainPem);
        }
        
        // 비밀번호 수정
        if (password != null) {
            certificate.setPassword(password);
        }
        
        // 관리자 수정
        if (admin != null) {
            certificate.setAdmin(admin);
        }
        
        // 알림 일수 수정
        if (alertDaysBeforeExpiry != null) {
            certificate.setAlertDaysBeforeExpiry(alertDaysBeforeExpiry);
        }
        
        // 자동 배포 여부 수정
        if (autoDeploy != null) {
            certificate.setAutoDeploy(autoDeploy);
        }
        
        certificate.setUpdatedAt(LocalDateTime.now());
        
        Certificate updated = certificateRepository.save(certificate);
        log.info("Certificate updated: {}", id);
        
        return updated;
    }

    /**
     * 인증서 삭제
     * 인증서 삭제 전에 연관된 배포 이력을 먼저 삭제합니다.
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting certificate: {}", id);

        Certificate certificate = findById(id);
        
        // 1. 연관된 배포 이력 삭제 (외래 키 제약 조건 위반 방지)
        List<Deployment> deployments = deploymentRepository.findByCertificateIdOrderByDeployedAtDesc(
            id, 
            Pageable.unpaged()
        ).getContent();
        
        if (!deployments.isEmpty()) {
            log.info("Deleting {} deployment records associated with certificate: {}", 
                deployments.size(), id);
            deploymentRepository.deleteAll(deployments);
        }
        
        // 2. 이메일 알림 발송 (삭제 전에 도메인 정보 저장)
        String domain = certificate.getDomain();
        
        // 3. 인증서 삭제
        certificateRepository.delete(certificate);

        // 4. 이메일 알림 발송
        emailService.ifPresent(service -> service.sendCertificateDeleted(domain));

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

    /**
     * 서버에 인증서 배포
     * 
     * @param certificate 배포할 인증서
     */
    private void deployToServer(Certificate certificate) {
        try {
            log.info("Starting deployment for certificate: {}", certificate.getId());
            
            // 배포 준비 상태 확인
            if (!distributionService.isReadyForDeployment(certificate)) {
                log.warn("Certificate {} is not ready for deployment", certificate.getId());
                return;
            }

            // 개인키 복호화
            String decryptedPrivateKey = decryptPrivateKey(certificate);

            // 배포 실행
            boolean deploymentSuccess = distributionService.deploy(certificate, decryptedPrivateKey);
            
            if (deploymentSuccess) {
                log.info("Certificate {} deployed successfully to server {}", 
                    certificate.getId(), 
                    certificate.getServer().getName());
            } else {
                log.error("Failed to deploy certificate {} to server {}", 
                    certificate.getId(), 
                    certificate.getServer().getName());
            }
            
        } catch (Exception e) {
            log.error("Error during certificate deployment for certificate: {}", 
                certificate.getId(), e);
            // 배포 실패는 인증서 생성/갱신을 실패시키지 않음
            // 나중에 수동으로 재배포 가능
        }
    }

    /**
     * 인증서의 최신 배포 상태 조회
     * 
     * @param certificateId 인증서 ID
     * @return 최신 배포 상태 (상태 없으면 "UNKNOWN" 반환)
     */
    public String getLatestDeploymentStatus(Long certificateId) {
        Page<Deployment> deploymentPage = deploymentRepository.findByCertificateIdOrderByDeployedAtDesc(
            certificateId, PageRequest.of(0, 1));
        
        if (deploymentPage.hasContent()) {
            Deployment latestDeployment = deploymentPage.getContent().get(0);
            return latestDeployment.getStatus().name();
        }
        
        return "UNKNOWN";
    }

    /**
     * 인증서 수동 배포
     * 
     * @param id 인증서 ID
     * @return 배포 이력
     */
    @Transactional
    public Deployment deployManually(Long id) {
        log.info("Manual deployment requested for certificate: {}", id);
        
        Certificate certificate = findById(id);
        
        // 인증서 상태 확인
        if (certificate.getStatus() != ACTIVE) {
            throw new IllegalStateException("활성화된 인증서만 배포할 수 있습니다. 현재 상태: " + certificate.getStatus());
        }
        
        // 서버 확인
        if (certificate.getServer() == null) {
            throw new IllegalStateException("서버가 등록되지 않은 인증서는 배포할 수 없습니다.");
        }
        
        // 배포 실행
        deployToServer(certificate);
        
        // 최신 배포 이력 조회 및 반환
        Page<Deployment> deploymentPage = deploymentRepository.findByCertificateIdOrderByDeployedAtDesc(
            id, PageRequest.of(0, 1));
        
        if (deploymentPage.hasContent()) {
            return deploymentPage.getContent().get(0);
        }
        
        throw new IllegalStateException("배포 이력을 찾을 수 없습니다.");
    }

}

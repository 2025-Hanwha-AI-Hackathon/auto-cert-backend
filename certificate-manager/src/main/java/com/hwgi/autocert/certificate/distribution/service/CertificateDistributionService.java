package com.hwgi.autocert.certificate.distribution.service;

import com.hwgi.autocert.certificate.distribution.config.DistributionProperties;
import com.hwgi.autocert.certificate.distribution.ssh.SshClient;
import com.hwgi.autocert.domain.model.Certificate;
import com.hwgi.autocert.domain.model.Deployment;
import com.hwgi.autocert.domain.model.DeploymentStatus;
import com.hwgi.autocert.domain.model.Server;
import com.hwgi.autocert.domain.repository.DeploymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증서 배포 서비스
 * SSH/SFTP를 통한 서버 배포 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateDistributionService {

    private final SshClient sshClient;
    private final DeploymentRepository deploymentRepository;
    private final DistributionProperties properties;

    /**
     * 서버에 인증서 배포
     *
     * @param certificate 배포할 인증서
     * @param decryptedPrivateKey 복호화된 개인키
     * @return 배포 성공 여부
     */
    @Transactional
    public boolean deploy(Certificate certificate, String decryptedPrivateKey) {
        Server server = certificate.getServer();

        if (server == null) {
            log.error("Certificate {} has no associated server", certificate.getId());
            return false;
        }

        log.info("Starting deployment of certificate {} to server {} ({}:{})",
                certificate.getId(),
                server.getName(),
                server.getIpAddress(),
                server.getPort());

        long startTime = System.currentTimeMillis();
        Deployment deployment = createDeployment(certificate, server, DeploymentStatus.IN_PROGRESS);

        SSHClient ssh = null;
        try {
            // 1. SSH 연결
            ssh = connectWithRetry(server);

            // 2. 배포 경로 결정
            String deployPath = server.getDeployPath() != null
                ? server.getDeployPath()
                : properties.getSsh().getDefaultCertPath();

            String certPath = deployPath + "/" + certificate.getDomain() + ".crt";
            String keyPath = deployPath + "/" + certificate.getDomain() + ".key";
            String chainPath = deployPath + "/" + certificate.getDomain() + "-chain.crt";

            // 3. 인증서 파일 업로드
            log.info("Uploading certificate files to {} - Certificate ID: {}, Domain: {}, IssuedAt: {}, ExpiresAt: {}", 
                deployPath, certificate.getId(), certificate.getDomain(), 
                certificate.getIssuedAt(), certificate.getExpiresAt());
            sshClient.uploadContent(ssh, certificate.getCertificatePem(), certPath);
            sshClient.uploadContent(ssh, decryptedPrivateKey, keyPath);

            if (certificate.getChainPem() != null && !certificate.getChainPem().isEmpty()) {
                sshClient.uploadContent(ssh, certificate.getChainPem(), chainPath);
            }

            // 4. 배포 성공 기록
            long duration = System.currentTimeMillis() - startTime;
            updateDeploymentStatus(deployment, DeploymentStatus.SUCCESS, deployPath,
                "Successfully deployed certificate files", duration);

            log.info("Certificate {} deployed successfully to server {} in {}ms",
                    certificate.getId(), server.getName(), duration);

            // 5. Nginx 재기동 (서버 타입이 NGINX인 경우)
            reloadNginxIfNeeded(ssh, server);

            return true;

        } catch (Exception e) {
            log.error("Failed to deploy certificate {} to server {}: {}",
                    certificate.getId(), server.getName(), e.getMessage(), e);

            long duration = System.currentTimeMillis() - startTime;
            updateDeploymentStatus(deployment, DeploymentStatus.FAILED, null,
                "Deployment failed: " + e.getMessage(), duration);

            return false;

        } finally {
            sshClient.disconnect(ssh);
        }
    }

    /**
     * 재시도 로직을 포함한 SSH 연결
     *
     * @param server 서버
     * @return SSH 클라이언트
     */
    private SSHClient connectWithRetry(Server server) throws Exception {
        int maxRetries = properties.getSsh().getMaxRetries();
        int retryDelay = properties.getSsh().getRetryDelay();

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("SSH connection attempt {}/{} to {}", attempt, maxRetries, server.getIpAddress());

                return sshClient.connect(
                    server.getIpAddress(),
                    server.getPort(),
                    server.getUsername(),
                    server.getPassword()
                );

            } catch (Exception e) {
                lastException = e;
                log.warn("SSH connection attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    Thread.sleep(retryDelay * attempt); // Exponential backoff
                }
            }
        }

        throw new Exception("Failed to connect after " + maxRetries + " attempts", lastException);
    }

    /**
     * 배포 이력 생성
     *
     * @param certificate 인증서
     * @param server 서버
     * @param status 상태
     * @return 배포 이력
     */
    private Deployment createDeployment(Certificate certificate, Server server, DeploymentStatus status) {
        Deployment deployment = Deployment.builder()
                .certificate(certificate)
                .server(server)
                .deployedAt(LocalDateTime.now())
                .status(status)
                .build();

        return deploymentRepository.save(deployment);
    }

    /**
     * 배포 상태 업데이트
     *
     * @param deployment 배포 이력
     * @param status 상태
     * @param deploymentPath 배포 경로
     * @param message 메시지
     * @param durationMs 소요 시간
     */
    private void updateDeploymentStatus(Deployment deployment, DeploymentStatus status,
                                       String deploymentPath, String message, long durationMs) {
        deployment.setStatus(status);
        deployment.setDeploymentPath(deploymentPath);
        deployment.setMessage(message);
        deployment.setDurationMs(durationMs);
        deploymentRepository.save(deployment);
    }

    /**
     * Nginx 재기동 (서버 타입이 NGINX인 경우에만)
     *
     * @param ssh SSH 클라이언트
     * @param server 서버
     */
    private void reloadNginxIfNeeded(SSHClient ssh, Server server) {
        // Nginx 서버가 아닌 경우 스킵
        if (!"nginx".equalsIgnoreCase(server.getWebServerType().getCode())) {
            log.debug("Server {} is not Nginx type, skipping reload", server.getName());
            return;
        }

        try {
            log.info("Starting Nginx reload for server {}", server.getName());

            // 1. Nginx 설정 테스트 (sudo 비밀번호 자동 입력)
            log.debug("Testing Nginx configuration");
            String testResult = sshClient.executeSudoCommand(ssh, "nginx -t", server.getPassword());
            log.info("Nginx configuration test passed: {}", testResult);

            // 2. Nginx 재기동 (graceful reload with sudo)
            log.debug("Reloading Nginx service");
            String reloadResult = sshClient.executeSudoCommand(ssh, "nginx -s reload", server.getPassword());
            log.info("Nginx reloaded successfully: {}", reloadResult);

        } catch (Exception e) {
            // Nginx 재기동 실패는 배포 전체를 실패시키지 않음
            // 인증서는 이미 배포되었으므로 수동 재기동 가능
            log.error("Failed to reload Nginx on server {}: {}", 
                server.getName(), e.getMessage(), e);
            log.warn("Certificate files are deployed, but Nginx reload failed. Manual reload required.");
        }
    }

    /**
     * 배포 준비 상태 확인
     *
     * @param certificate 확인할 인증서
     * @return 배포 가능 여부
     */
    public boolean isReadyForDeployment(Certificate certificate) {
        if (certificate == null) {
            log.warn("Certificate is null");
            return false;
        }

        if (certificate.getServer() == null) {
            log.warn("Certificate {} has no associated server", certificate.getId());
            return false;
        }

        if (certificate.getCertificatePem() == null || certificate.getCertificatePem().isEmpty()) {
            log.warn("Certificate {} has no certificate PEM", certificate.getId());
            return false;
        }

        if (certificate.getPrivateKeyPem() == null || certificate.getPrivateKeyPem().isEmpty()) {
            log.warn("Certificate {} has no private key PEM", certificate.getId());
            return false;
        }

        Server server = certificate.getServer();
        if (server.getIpAddress() == null || server.getIpAddress().isEmpty()) {
            log.warn("Server {} has no IP address", server.getId());
            return false;
        }

        if (server.getUsername() == null || server.getUsername().isEmpty()) {
            log.warn("Server {} has no username", server.getId());
            return false;
        }

        if (server.getPassword() == null || server.getPassword().isEmpty()) {
            log.warn("Server {} has no password", server.getId());
            return false;
        }

        log.debug("Certificate {} is ready for deployment", certificate.getId());
        return true;
    }
}

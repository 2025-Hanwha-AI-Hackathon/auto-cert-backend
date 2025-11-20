package com.hwgi.autocert.certificate.distribution.ssh;

import com.hwgi.autocert.certificate.distribution.config.DistributionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SSH/SFTP 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshClient {

    private final DistributionProperties properties;

    /**
     * SSH 연결 생성
     *
     * @param host 호스트
     * @param port 포트
     * @param username 사용자명
     * @param password 비밀번호
     * @return SSH 클라이언트
     */
    public SSHClient connect(String host, int port, String username, String password) throws IOException {
        log.debug("Connecting to SSH server: {}@{}:{}", username, host, port);

        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier()); // Production에서는 실제 호스트 키 검증 필요
        ssh.setTimeout(properties.getSsh().getTimeout());

        ssh.connect(host, port);
        ssh.authPassword(username, password);

        log.info("SSH connection established: {}@{}:{}", username, host, port);
        return ssh;
    }

    /**
     * 파일 업로드
     *
     * @param ssh SSH 클라이언트
     * @param localFilePath 로컬 파일 경로
     * @param remoteFilePath 원격 파일 경로
     */
    public void uploadFile(SSHClient ssh, String localFilePath, String remoteFilePath) throws IOException {
        log.debug("Uploading file: {} -> {}", localFilePath, remoteFilePath);

        try (SFTPClient sftp = ssh.newSFTPClient()) {
            // 원격 디렉토리 생성
            String remoteDir = remoteFilePath.substring(0, remoteFilePath.lastIndexOf('/'));
            createRemoteDirectory(sftp, remoteDir);

            // 파일 업로드
            sftp.put(new FileSystemFile(localFilePath), remoteFilePath);

            // 파일 권한 설정 (600 - owner read/write only)
            sftp.chmod(remoteFilePath, 0600);

            log.info("File uploaded successfully: {}", remoteFilePath);
        }
    }

    /**
     * 문자열 내용을 원격 파일로 업로드
     *
     * @param ssh SSH 클라이언트
     * @param content 파일 내용
     * @param remoteFilePath 원격 파일 경로
     */
    public void uploadContent(SSHClient ssh, String content, String remoteFilePath) throws IOException {
        log.debug("Uploading content to: {}", remoteFilePath);

        // 임시 파일 생성
        Path tempFile = Files.createTempFile("autocert-", ".tmp");
        try {
            Files.writeString(tempFile, content);
            uploadFile(ssh, tempFile.toString(), remoteFilePath);
        } finally {
            Files.deleteIfExists(tempFile);
        }

        log.info("Content uploaded successfully: {}", remoteFilePath);
    }

    /**
     * 원격 디렉토리 생성
     *
     * @param sftp SFTP 클라이언트
     * @param remotePath 원격 디렉토리 경로
     */
    private void createRemoteDirectory(SFTPClient sftp, String remotePath) throws IOException {
        try {
            sftp.statExistence(remotePath);
            log.debug("Remote directory already exists: {}", remotePath);
        } catch (IOException e) {
            log.debug("Creating remote directory: {}", remotePath);
            sftp.mkdirs(remotePath);
        }
    }

    /**
     * 명령 실행
     *
     * @param ssh SSH 클라이언트
     * @param command 실행할 명령
     * @return 명령 출력
     */
    public String executeCommand(SSHClient ssh, String command) throws IOException {
        log.debug("Executing command: {}", command);

        try (var session = ssh.startSession();
             var cmd = session.exec(command)) {

            String output = new String(cmd.getInputStream().readAllBytes());
            String error = new String(cmd.getErrorStream().readAllBytes());

            cmd.join(properties.getSsh().getTimeout(), java.util.concurrent.TimeUnit.MILLISECONDS);

            int exitStatus = cmd.getExitStatus();

            if (exitStatus != 0) {
                log.warn("Command failed with exit code {}: {}", exitStatus, error);
                throw new IOException("Command execution failed: " + error);
            }

            log.info("Command executed successfully: {}", command);
            return output;
        }
    }

    /**
     * 연결 종료
     *
     * @param ssh SSH 클라이언트
     */
    public void disconnect(SSHClient ssh) {
        if (ssh != null && ssh.isConnected()) {
            try {
                ssh.disconnect();
                log.debug("SSH connection closed");
            } catch (IOException e) {
                log.warn("Error closing SSH connection", e);
            }
        }
    }
}

package com.hwgi.autocert.certificate.service;

import com.hwgi.autocert.certificate.config.AcmeProperties;
import com.hwgi.autocert.common.exception.ResourceNotFoundException;
import com.hwgi.autocert.domain.model.AcmeAccount;
import com.hwgi.autocert.domain.repository.AcmeAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.Security;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ACME 계정 관리 서비스
 * 
 * Let's Encrypt 등 ACME 프로토콜을 사용하는 CA와의 계정 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcmeAccountService {

    private final AcmeAccountRepository acmeAccountRepository;
    private final AcmeProperties acmeProperties;

    static {
        // Bouncy Castle 프로바이더 등록
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 기본 ACME 계정 조회 또는 생성
     * 
     * @return ACME 계정
     */
    @Transactional
    public AcmeAccount getOrCreateDefaultAccount() {
        log.debug("Getting or creating default ACME account");

        // 기존 계정 조회 (이메일 + 서버 URL 조합)
        Optional<AcmeAccount> existingAccount = acmeAccountRepository
                .findByEmailAndAcmeServerUrl(acmeProperties.getAccountEmail(), acmeProperties.getDirectoryUrl());

        if (existingAccount.isPresent()) {
            AcmeAccount account = existingAccount.get();
            account.updateLastUsed();
            log.info("Using existing ACME account: {}", account.getEmail());
            return acmeAccountRepository.save(account);
        }

        // 새 계정 생성
        return createAccount(acmeProperties.getAccountEmail(), acmeProperties.getDirectoryUrl());
    }

    /**
     * ACME 계정 생성 및 등록
     * 
     * @param email ACME 계정 이메일
     * @param acmeServerUrl ACME 서버 URL
     * @return 생성된 ACME 계정
     */
    @Transactional
    public AcmeAccount createAccount(String email, String acmeServerUrl) {
        log.info("Creating new ACME account for email: {}, server: {}", email, acmeServerUrl);

        try {
            // 1. 키페어 생성
            KeyPair accountKeyPair = generateKeyPair();

            // 2. ACME 세션 생성
            Session session = new Session(acmeServerUrl);

            // 3. ACME 계정 등록
            AccountBuilder accountBuilder = new AccountBuilder()
                    .addContact("mailto:" + email)
                    .agreeToTermsOfService()
                    .useKeyPair(accountKeyPair);

            Account account = accountBuilder.create(session);
            log.info("ACME account created successfully. Account URL: {}", account.getLocation());

            // 4. 키페어를 PEM 형식으로 변환
            String privateKeyPem = serializePrivateKey(accountKeyPair);
            String publicKeyPem = serializePublicKey(accountKeyPair);

            // 5. DB에 저장
            AcmeAccount acmeAccount = AcmeAccount.builder()
                    .email(email)
                    .acmeServerUrl(acmeServerUrl)
                    .accountUrl(account.getLocation().toString())
                    .privateKeyPem(privateKeyPem)
                    .publicKeyPem(publicKeyPem)
                    .status("ACTIVE")
                    .keyAlgorithm(acmeProperties.getKeyAlgorithm())
                    .keySize(acmeProperties.getKeySize())
                    .termsAgreed(true)
                    .lastUsedAt(LocalDateTime.now())
                    .build();

            AcmeAccount saved = acmeAccountRepository.save(acmeAccount);
            log.info("ACME account saved to database with id: {}", saved.getId());

            return saved;

        } catch (Exception e) {
            log.error("Failed to create ACME account for email: {}", email, e);
            throw new RuntimeException("ACME 계정 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * ACME Session 생성 (기존 계정 사용)
     * 
     * @param acmeAccount ACME 계정
     * @return ACME Account 객체
     */
    public Account getAcmeAccount(AcmeAccount acmeAccount) {
        log.debug("Creating ACME session for account: {}", acmeAccount.getEmail());

        try {
            // 1. 저장된 키페어 복원
            KeyPair accountKeyPair = deserializeKeyPair(acmeAccount.getPrivateKeyPem());

            // 2. ACME 세션 생성
            Session session = new Session(acmeAccount.getAcmeServerUrl());

            // 3. 기존 계정 로그인
            Account account = new AccountBuilder()
                    .useKeyPair(accountKeyPair)
                    .onlyExisting()
                    .create(session);

            log.debug("ACME account session created successfully");
            return account;

        } catch (Exception e) {
            log.error("Failed to create ACME session for account: {}", acmeAccount.getEmail(), e);
            throw new RuntimeException("ACME 세션 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * ID로 ACME 계정 조회
     */
    public AcmeAccount findById(Long id) {
        return acmeAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ACME 계정을 찾을 수 없습니다: " + id));
    }

    /**
     * 이메일로 ACME 계정 조회
     */
    public Optional<AcmeAccount> findByEmail(String email) {
        return acmeAccountRepository.findByEmail(email);
    }

    /**
     * 모든 활성 ACME 계정 조회
     */
    public List<AcmeAccount> findAllActiveAccounts() {
        return acmeAccountRepository.findByStatus("ACTIVE");
    }

    /**
     * ACME 계정 비활성화
     */
    @Transactional
    public void deactivateAccount(Long id) {
        log.info("Deactivating ACME account: {}", id);

        AcmeAccount account = findById(id);
        account.setStatus("DEACTIVATED");
        account.setUpdatedAt(LocalDateTime.now());

        acmeAccountRepository.save(account);
        log.info("ACME account deactivated: {}", id);
    }

    /**
     * 키페어 생성
     */
    private KeyPair generateKeyPair() throws Exception {
        String algorithm = acmeProperties.getKeyAlgorithm();
        int keySize = acmeProperties.getKeySize();

        log.debug("Generating key pair: algorithm={}, size={}", algorithm, keySize);

        if ("RSA".equalsIgnoreCase(algorithm)) {
            return KeyPairUtils.createKeyPair(keySize);
        } else if ("ECDSA".equalsIgnoreCase(algorithm)) {
            return KeyPairUtils.createECKeyPair("secp" + keySize + "r1");
        } else {
            throw new IllegalArgumentException("Unsupported key algorithm: " + algorithm);
        }
    }

    /**
     * 개인키를 PEM 형식으로 직렬화
     */
    private String serializePrivateKey(KeyPair keyPair) throws IOException {
        StringWriter writer = new StringWriter();
        KeyPairUtils.writeKeyPair(keyPair, writer);
        return writer.toString();
    }

    /**
     * 공개키를 PEM 형식으로 직렬화
     */
    private String serializePublicKey(KeyPair keyPair) throws IOException {
        // 공개키는 개인키에서 추출 가능하므로 전체 KeyPair를 저장
        return serializePrivateKey(keyPair);
    }

    /**
     * PEM 형식에서 KeyPair 역직렬화
     */
    private KeyPair deserializeKeyPair(String privateKeyPem) throws IOException {
        StringReader reader = new StringReader(privateKeyPem);
        return KeyPairUtils.readKeyPair(reader);
    }
}

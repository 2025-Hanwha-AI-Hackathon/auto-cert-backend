package com.hwgi.autocert.certificate.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
public class CertificateEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final int AES_KEY_SIZE = 256; // 256 bits

    private final String encryptionKey;

    /**
     * 생성자 - application.yml에서 암호화 키 주입
     * 
     * @param encryptionKey 암호화 키 (Base64 인코딩)
     * @throws IllegalStateException 암호화 키가 설정되지 않았거나 유효하지 않은 경우
     */
    public CertificateEncryptionUtil(@Value("${autocert.encryption.key}") String encryptionKey) {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            log.error("autocert.encryption.key is not configured!");
            log.error("Please set CERTIFICATE_ENCRYPTION_KEY environment variable.");
            log.error("Generate a new key with: openssl rand -base64 32");
            throw new IllegalStateException(
                "암호화 키가 설정되지 않았습니다. " +
                "CERTIFICATE_ENCRYPTION_KEY 환경변수를 반드시 설정해야 합니다. " +
                "키 생성: openssl rand -base64 32"
            );
        }

        // 키 유효성 검증
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            if (keyBytes.length != AES_KEY_SIZE / 8) {
                throw new IllegalStateException(
                    String.format("암호화 키 길이가 올바르지 않습니다. 필요: %d bytes, 실제: %d bytes", 
                        AES_KEY_SIZE / 8, keyBytes.length)
                );
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("암호화 키가 올바른 Base64 형식이 아닙니다.", e);
        }

        this.encryptionKey = encryptionKey;
        log.info("Certificate encryption utility initialized successfully");
    }

    /**
     * 개인키 암호화
     * 
     * @param privateKeyPem PEM 형식의 개인키
     * @return Base64 인코딩된 암호화 데이터 (IV + 암호문)
     */
    public String encrypt(String privateKeyPem) {
        try {
            // 암호화 키 로드
            SecretKey secretKey = loadEncryptionKey();

            // IV 생성 (매번 랜덤)
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // 암호화
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedData = cipher.doFinal(privateKeyPem.getBytes());

            // IV + 암호문 결합
            byte[] combined = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

            // Base64 인코딩
            String encrypted = Base64.getEncoder().encodeToString(combined);
            log.debug("Private key encrypted successfully");

            return encrypted;

        } catch (Exception e) {
            log.error("Failed to encrypt private key", e);
            throw new RuntimeException("암호화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 개인키 복호화
     * 
     * @param encryptedData Base64 인코딩된 암호화 데이터
     * @return PEM 형식의 개인키
     */
    public String decrypt(String encryptedData) {
        try {
            // 암호화 키 로드
            SecretKey secretKey = loadEncryptionKey();

            // Base64 디코딩
            byte[] combined = Base64.getDecoder().decode(encryptedData);

            // IV 추출
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            // 암호문 추출
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            // 복호화
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedData = cipher.doFinal(ciphertext);
            String privateKeyPem = new String(decryptedData);

            log.debug("Private key decrypted successfully");
            return privateKeyPem;

        } catch (Exception e) {
            log.error("Failed to decrypt private key", e);
            throw new RuntimeException("복호화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 암호화 키 로드
     */
    private SecretKey loadEncryptionKey() {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 데이터가 암호화되었는지 확인
     * 
     * @param data 검사할 데이터
     * @return 암호화 여부
     */
    public boolean isEncrypted(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        // PEM 형식이면 암호화되지 않은 것
        if (data.startsWith("-----BEGIN")) {
            return false;
        }

        // Base64 형식이면 암호화된 것으로 간주
        try {
            Base64.getDecoder().decode(data);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

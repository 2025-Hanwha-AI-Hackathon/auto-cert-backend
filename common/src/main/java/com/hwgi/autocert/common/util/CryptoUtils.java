package com.hwgi.autocert.common.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 암호화/복호화 유틸리티
 * AES-256-GCM 암호화 알고리즘 사용
 */
public final class CryptoUtils {

    private CryptoUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * AES-256 암호화 키 생성
     */
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE, new SecureRandom());
        return keyGenerator.generateKey();
    }

    /**
     * Base64 인코딩된 키 문자열로부터 SecretKey 생성
     */
    public static SecretKey getKeyFromString(String keyString) {
        byte[] decodedKey = Base64.decodeBase64(keyString);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    /**
     * SecretKey를 Base64 문자열로 변환
     */
    public static String keyToString(SecretKey key) {
        return Base64.encodeBase64String(key.getEncoded());
    }

    /**
     * 데이터 암호화
     *
     * @param plainText 평문
     * @param secretKey 암호화 키
     * @return Base64 인코딩된 암호문 (IV + 암호문)
     */
    public static String encrypt(String plainText, SecretKey secretKey) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // IV와 암호문을 결합
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);

        return Base64.encodeBase64String(byteBuffer.array());
    }

    /**
     * 데이터 복호화
     *
     * @param cipherText Base64 인코딩된 암호문 (IV + 암호문)
     * @param secretKey  복호화 키
     * @return 평문
     */
    public static String decrypt(String cipherText, SecretKey secretKey) throws Exception {
        byte[] cipherMessage = Base64.decodeBase64(cipherText);
        ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);

        // IV 추출
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);

        // 암호문 추출
        byte[] encrypted = new byte[byteBuffer.remaining()];
        byteBuffer.get(encrypted);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        byte[] plainText = cipher.doFinal(encrypted);
        return new String(plainText, StandardCharsets.UTF_8);
    }

    /**
     * SHA-256 해시 생성
     */
    public static String sha256(String input) {
        return DigestUtils.sha256Hex(input);
    }

    /**
     * MD5 해시 생성 (레거시 시스템 호환용)
     */
    public static String md5(String input) {
        return DigestUtils.md5Hex(input);
    }

    /**
     * 보안 랜덤 문자열 생성
     */
    public static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.encodeBase64URLSafeString(bytes).substring(0, length);
    }
}
package com.hwgi.autocert.common.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 입력값 검증 유틸리티
 */
public final class ValidationUtils {

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // 정규식 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$"
    );

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    private static final Pattern PORT_PATTERN = Pattern.compile(
            "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$"
    );

    /**
     * null이 아닌지 확인
     */
    public static void requireNonNull(Object object, String fieldName) {
        if (object == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    /**
     * 빈 문자열이 아닌지 확인
     */
    public static void requireNonBlank(String str, String fieldName) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    /**
     * 컬렉션이 비어있지 않은지 확인
     */
    public static void requireNonEmpty(Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
    }

    /**
     * 배열이 비어있지 않은지 확인
     */
    public static void requireNonEmpty(Object[] array, String fieldName) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
    }

    /**
     * 이메일 주소 유효성 검증
     */
    public static boolean isValidEmail(String email) {
        return StringUtils.isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 도메인 이름 유효성 검증
     */
    public static boolean isValidDomain(String domain) {
        return StringUtils.isNotBlank(domain) && DOMAIN_PATTERN.matcher(domain).matches();
    }

    /**
     * IP 주소 유효성 검증
     */
    public static boolean isValidIpAddress(String ip) {
        return StringUtils.isNotBlank(ip) && IP_PATTERN.matcher(ip).matches();
    }

    /**
     * 포트 번호 유효성 검증 (1-65535)
     */
    public static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    /**
     * 포트 번호 유효성 검증 (문자열)
     */
    public static boolean isValidPort(String port) {
        return StringUtils.isNotBlank(port) && PORT_PATTERN.matcher(port).matches();
    }

    /**
     * 범위 내 값인지 확인
     */
    public static void requireInRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    String.format("%s must be between %d and %d", fieldName, min, max)
            );
        }
    }

    /**
     * 양수인지 확인
     */
    public static void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    /**
     * 음수가 아닌지 확인
     */
    public static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }

    /**
     * 최소 길이 확인
     */
    public static void requireMinLength(String str, int minLength, String fieldName) {
        if (str == null || str.length() < minLength) {
            throw new IllegalArgumentException(
                    String.format("%s must be at least %d characters", fieldName, minLength)
            );
        }
    }

    /**
     * 최대 길이 확인
     */
    public static void requireMaxLength(String str, int maxLength, String fieldName) {
        if (str != null && str.length() > maxLength) {
            throw new IllegalArgumentException(
                    String.format("%s must not exceed %d characters", fieldName, maxLength)
            );
        }
    }
}
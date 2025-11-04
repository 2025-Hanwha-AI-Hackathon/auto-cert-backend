package com.hwgi.autocert.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationUtils 테스트")
class ValidationUtilsTest {

    @Test
    @DisplayName("이메일 유효성 검증 - 유효한 이메일")
    void testIsValidEmail_Valid() {
        assertTrue(ValidationUtils.isValidEmail("test@example.com"));
        assertTrue(ValidationUtils.isValidEmail("user.name+tag@example.co.kr"));
    }

    @Test
    @DisplayName("이메일 유효성 검증 - 유효하지 않은 이메일")
    void testIsValidEmail_Invalid() {
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
        assertFalse(ValidationUtils.isValidEmail("@example.com"));
        assertFalse(ValidationUtils.isValidEmail("test@"));
        assertFalse(ValidationUtils.isValidEmail(""));
        assertFalse(ValidationUtils.isValidEmail(null));
    }

    @Test
    @DisplayName("도메인 유효성 검증 - 유효한 도메인")
    void testIsValidDomain_Valid() {
        assertTrue(ValidationUtils.isValidDomain("example.com"));
        assertTrue(ValidationUtils.isValidDomain("sub.example.com"));
        assertTrue(ValidationUtils.isValidDomain("my-domain.co.kr"));
    }

    @Test
    @DisplayName("도메인 유효성 검증 - 유효하지 않은 도메인")
    void testIsValidDomain_Invalid() {
        assertFalse(ValidationUtils.isValidDomain("invalid_domain"));
        assertFalse(ValidationUtils.isValidDomain("-example.com"));
        assertFalse(ValidationUtils.isValidDomain(""));
        assertFalse(ValidationUtils.isValidDomain(null));
    }

    @Test
    @DisplayName("IP 주소 유효성 검증 - 유효한 IP")
    void testIsValidIpAddress_Valid() {
        assertTrue(ValidationUtils.isValidIpAddress("192.168.1.1"));
        assertTrue(ValidationUtils.isValidIpAddress("10.0.0.1"));
        assertTrue(ValidationUtils.isValidIpAddress("255.255.255.255"));
    }

    @Test
    @DisplayName("IP 주소 유효성 검증 - 유효하지 않은 IP")
    void testIsValidIpAddress_Invalid() {
        assertFalse(ValidationUtils.isValidIpAddress("256.1.1.1"));
        assertFalse(ValidationUtils.isValidIpAddress("192.168.1"));
        assertFalse(ValidationUtils.isValidIpAddress("invalid-ip"));
        assertFalse(ValidationUtils.isValidIpAddress(""));
        assertFalse(ValidationUtils.isValidIpAddress(null));
    }

    @Test
    @DisplayName("포트 번호 유효성 검증 - 유효한 포트")
    void testIsValidPort_Valid() {
        assertTrue(ValidationUtils.isValidPort(80));
        assertTrue(ValidationUtils.isValidPort(443));
        assertTrue(ValidationUtils.isValidPort(8080));
        assertTrue(ValidationUtils.isValidPort(65535));
    }

    @Test
    @DisplayName("포트 번호 유효성 검증 - 유효하지 않은 포트")
    void testIsValidPort_Invalid() {
        assertFalse(ValidationUtils.isValidPort(0));
        assertFalse(ValidationUtils.isValidPort(-1));
        assertFalse(ValidationUtils.isValidPort(65536));
    }

    @Test
    @DisplayName("null 체크 - 정상")
    void testRequireNonNull_Success() {
        assertDoesNotThrow(() -> ValidationUtils.requireNonNull("test", "field"));
    }

    @Test
    @DisplayName("null 체크 - 실패")
    void testRequireNonNull_Failure() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ValidationUtils.requireNonNull(null, "field")
        );
        assertEquals("field must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("빈 문자열 체크 - 정상")
    void testRequireNonBlank_Success() {
        assertDoesNotThrow(() -> ValidationUtils.requireNonBlank("test", "field"));
    }

    @Test
    @DisplayName("빈 문자열 체크 - 실패")
    void testRequireNonBlank_Failure() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonBlank("", "field"));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonBlank("   ", "field"));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonBlank(null, "field"));
    }

    @Test
    @DisplayName("범위 체크 - 정상")
    void testRequireInRange_Success() {
        assertDoesNotThrow(() -> ValidationUtils.requireInRange(50, 1, 100, "value"));
    }

    @Test
    @DisplayName("범위 체크 - 실패")
    void testRequireInRange_Failure() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireInRange(0, 1, 100, "value"));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireInRange(101, 1, 100, "value"));
    }
}
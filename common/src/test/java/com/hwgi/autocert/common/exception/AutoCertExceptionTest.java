package com.hwgi.autocert.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AutoCertException 테스트")
class AutoCertExceptionTest {

    @Test
    @DisplayName("ErrorCode로 예외 생성")
    void testCreateWithErrorCode() {
        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;

        AutoCertException exception = new AutoCertException(errorCode);

        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(errorCode.getMessage(), exception.getMessage());
        assertNull(exception.getDetail());
    }

    @Test
    @DisplayName("ErrorCode와 detail로 예외 생성")
    void testCreateWithErrorCodeAndDetail() {
        ErrorCode errorCode = CommonErrorCode.VALIDATION_FAILED;
        String detail = "이메일 형식이 올바르지 않습니다";

        AutoCertException exception = new AutoCertException(errorCode, detail);

        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(detail, exception.getDetail());
        assertTrue(exception.getMessage().contains(detail));
    }

    @Test
    @DisplayName("ErrorCode와 cause로 예외 생성")
    void testCreateWithErrorCodeAndCause() {
        ErrorCode errorCode = CommonErrorCode.EXTERNAL_SERVICE_ERROR;
        Throwable cause = new RuntimeException("Connection failed");

        AutoCertException exception = new AutoCertException(errorCode, cause);

        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("메시지로 예외 생성")
    void testCreateWithMessage() {
        String message = "Custom error message";

        AutoCertException exception = new AutoCertException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrorCode());
    }

    @Test
    @DisplayName("CertificateException 생성")
    void testCertificateException() {
        ErrorCode errorCode = CommonErrorCode.VALIDATION_FAILED;

        CertificateException exception = new CertificateException(errorCode);

        assertInstanceOf(AutoCertException.class, exception);
        assertEquals(errorCode, exception.getErrorCode());
    }

    @Test
    @DisplayName("DistributionException 생성")
    void testDistributionException() {
        ErrorCode errorCode = CommonErrorCode.CONNECTION_ERROR;

        DistributionException exception = new DistributionException(errorCode);

        assertInstanceOf(AutoCertException.class, exception);
        assertEquals(errorCode, exception.getErrorCode());
    }

    @Test
    @DisplayName("ValidationException 생성")
    void testValidationException() {
        String message = "Validation failed";

        ValidationException exception = new ValidationException(message);

        assertInstanceOf(AutoCertException.class, exception);
        assertEquals(message, exception.getMessage());
    }
}
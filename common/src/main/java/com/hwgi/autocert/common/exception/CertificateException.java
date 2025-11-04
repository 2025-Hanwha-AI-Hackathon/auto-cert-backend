package com.hwgi.autocert.common.exception;

/**
 * 인증서 관련 예외
 * 인증서 발급, 갱신, 검증 등에서 발생하는 예외
 */
public class CertificateException extends AutoCertException {

    public CertificateException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CertificateException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public CertificateException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public CertificateException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    public CertificateException(String message) {
        super(message);
    }

    public CertificateException(String message, Throwable cause) {
        super(message, cause);
    }
}

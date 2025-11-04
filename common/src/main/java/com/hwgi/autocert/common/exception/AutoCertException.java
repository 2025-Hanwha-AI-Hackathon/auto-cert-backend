package com.hwgi.autocert.common.exception;

import lombok.Getter;

/**
 * Auto-Cert 시스템의 최상위 예외 클래스
 * 모든 커스텀 예외는 이 클래스를 상속
 */
@Getter
public class AutoCertException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    public AutoCertException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public AutoCertException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public AutoCertException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detail = null;
    }

    public AutoCertException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getMessage() + ": " + detail, cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public AutoCertException(String message) {
        super(message);
        this.errorCode = null;
        this.detail = null;
    }

    public AutoCertException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.detail = null;
    }
}

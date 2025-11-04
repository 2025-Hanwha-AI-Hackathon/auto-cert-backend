package com.hwgi.autocert.common.exception;

/**
 * 검증 관련 예외
 * 입력값 검증, 설정 검증 등에서 발생하는 예외
 */
public class ValidationException extends AutoCertException {

    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ValidationException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public ValidationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ValidationException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

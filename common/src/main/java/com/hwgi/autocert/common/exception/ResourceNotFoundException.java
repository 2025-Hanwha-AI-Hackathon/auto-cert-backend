package com.hwgi.autocert.common.exception;

/**
 * 리소스 찾기 실패 예외
 * 데이터베이스에서 엔티티를 찾을 수 없거나, 파일을 찾을 수 없을 때 발생
 */
public class ResourceNotFoundException extends AutoCertException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public ResourceNotFoundException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

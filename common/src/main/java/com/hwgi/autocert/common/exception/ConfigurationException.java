package com.hwgi.autocert.common.exception;

/**
 * 설정 관련 예외
 * 웹서버 설정, 애플리케이션 설정 등에서 발생하는 예외
 */
public class ConfigurationException extends AutoCertException {

    public ConfigurationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConfigurationException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public ConfigurationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ConfigurationException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}

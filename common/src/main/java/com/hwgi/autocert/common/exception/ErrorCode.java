package com.hwgi.autocert.common.exception;

/**
 * 에러 코드 인터페이스
 * 각 모듈에서 이 인터페이스를 구현하여 에러 코드를 정의
 */
public interface ErrorCode {

    /**
     * 에러 코드 반환
     *
     * @return 에러 코드 (예: "CERT_001")
     */
    String getCode();

    /**
     * 에러 메시지 반환
     *
     * @return 에러 메시지
     */
    String getMessage();

    /**
     * HTTP 상태 코드 반환
     *
     * @return HTTP 상태 코드 (기본값: 500)
     */
    default int getHttpStatus() {
        return 500;
    }
}

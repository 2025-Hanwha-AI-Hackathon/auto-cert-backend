package com.hwgi.autocert.common.exception;

/**
 * 배포 관련 예외
 * SSH 연결, 파일 전송 등 배포 과정에서 발생하는 예외
 */
public class DistributionException extends AutoCertException {

    public DistributionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DistributionException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public DistributionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public DistributionException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    public DistributionException(String message) {
        super(message);
    }

    public DistributionException(String message, Throwable cause) {
        super(message, cause);
    }
}

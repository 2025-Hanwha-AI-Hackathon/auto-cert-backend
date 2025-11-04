package com.hwgi.autocert.common.constants;

/**
 * 배포 관련 상수
 */
public final class DeploymentConstants {

    private DeploymentConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // 배포 상태
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_ROLLBACK = "ROLLBACK";

    // 배포 타입
    public static final String TYPE_INITIAL = "INITIAL";
    public static final String TYPE_RENEWAL = "RENEWAL";
    public static final String TYPE_EMERGENCY = "EMERGENCY";

    // 파일 전송 모드
    public static final int FILE_MODE_READ_WRITE = 0600; // rw-------
    public static final int FILE_MODE_READ_ALL = 0644;   // rw-r--r--

    // 배포 전략
    public static final String STRATEGY_SEQUENTIAL = "SEQUENTIAL"; // 순차 배포
    public static final String STRATEGY_PARALLEL = "PARALLEL";     // 병렬 배포
    public static final String STRATEGY_CANARY = "CANARY";         // 카나리 배포
    public static final String STRATEGY_BLUE_GREEN = "BLUE_GREEN"; // 블루-그린 배포

    // 배포 타임아웃 (밀리초)
    public static final int DEPLOYMENT_TIMEOUT = 300000; // 5분
    public static final int FILE_TRANSFER_TIMEOUT = 60000; // 1분

    // 재시도 설정
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final int RETRY_DELAY_MS = 2000; // 2초
    public static final double RETRY_BACKOFF_MULTIPLIER = 2.0;

    // 롤백 설정
    public static final boolean AUTO_ROLLBACK_ENABLED = true;
    public static final int ROLLBACK_TIMEOUT = 60000; // 1분

    // 파일 권한
    public static final String OWNER_READ_WRITE = "600";
    public static final String OWNER_READ_WRITE_EXECUTE = "700";
    public static final String ALL_READ_OWNER_WRITE = "644";
}
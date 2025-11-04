package com.hwgi.autocert.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공통 에러 코드
 * 모든 모듈에서 공통으로 사용하는 에러 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // 일반 에러 (1xxx)
    INTERNAL_SERVER_ERROR("COMMON_1001", "내부 서버 오류가 발생했습니다", 500),
    INVALID_INPUT("COMMON_1002", "잘못된 입력값입니다", 400),
    RESOURCE_NOT_FOUND("COMMON_1003", "요청한 리소스를 찾을 수 없습니다", 404),
    UNAUTHORIZED("COMMON_1004", "인증이 필요합니다", 401),
    FORBIDDEN("COMMON_1005", "접근 권한이 없습니다", 403),

    // 데이터 검증 에러 (2xxx)
    VALIDATION_FAILED("COMMON_2001", "데이터 검증에 실패했습니다", 400),
    REQUIRED_FIELD_MISSING("COMMON_2002", "필수 필드가 누락되었습니다", 400),
    INVALID_FORMAT("COMMON_2003", "잘못된 형식입니다", 400),
    DUPLICATE_RESOURCE("COMMON_2004", "중복된 리소스가 이미 존재합니다", 409),

    // 설정 에러 (3xxx)
    CONFIGURATION_ERROR("COMMON_3001", "설정 오류가 발생했습니다", 500),
    INVALID_CONFIGURATION("COMMON_3002", "잘못된 설정입니다", 500),

    // 외부 시스템 연동 에러 (4xxx)
    EXTERNAL_SERVICE_ERROR("COMMON_4001", "외부 서비스 호출 중 오류가 발생했습니다", 502),
    TIMEOUT_ERROR("COMMON_4002", "요청 시간이 초과되었습니다", 504),
    CONNECTION_ERROR("COMMON_4003", "연결 오류가 발생했습니다", 503);

    private final String code;
    private final String message;
    private final int httpStatus;
}

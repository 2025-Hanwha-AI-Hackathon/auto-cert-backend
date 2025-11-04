package com.hwgi.autocert.common.constants;

/**
 * Auto-Cert 시스템 전역 상수
 */
public final class AutoCertConstants {

    private AutoCertConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // 애플리케이션 정보
    public static final String APP_NAME = "Auto-Cert";
    public static final String APP_VERSION = "1.0.0";
    public static final String APP_DESCRIPTION = "SSL/TLS Certificate Management Automation";

    // 인코딩
    public static final String DEFAULT_ENCODING = "UTF-8";

    // 날짜/시간 포맷
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // HTTP 헤더
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_USER_AGENT = "User-Agent";

    // Content Type
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_XML = "application/xml";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    // 기본 타임아웃 (밀리초)
    public static final int DEFAULT_TIMEOUT = 30000; // 30초
    public static final int LONG_TIMEOUT = 60000; // 60초

    // 재시도 설정
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final int DEFAULT_RETRY_DELAY = 1000; // 1초

    // 페이징
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // 파일 크기 제한
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // 성공/실패 플래그
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";

    // Yes/No
    public static final String YES = "Y";
    public static final String NO = "N";
}
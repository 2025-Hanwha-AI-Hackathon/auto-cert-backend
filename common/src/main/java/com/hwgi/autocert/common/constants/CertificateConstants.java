package com.hwgi.autocert.common.constants;

/**
 * 인증서 관련 상수
 */
public final class CertificateConstants {

    private CertificateConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // 인증서 타입
    public static final String TYPE_SINGLE = "SINGLE";
    public static final String TYPE_WILDCARD = "WILDCARD";
    public static final String TYPE_MULTI_DOMAIN = "MULTI_DOMAIN";

    // 인증서 상태
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_REVOKED = "REVOKED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FAILED = "FAILED";

    // 챌린지 타입
    public static final String CHALLENGE_HTTP_01 = "HTTP-01";
    public static final String CHALLENGE_DNS_01 = "DNS-01";
    public static final String CHALLENGE_TLS_ALPN_01 = "TLS-ALPN-01";

    // ACME 프로바이더
    public static final String PROVIDER_LETSENCRYPT = "LETSENCRYPT";
    public static final String PROVIDER_ZEROSSL = "ZEROSSL";
    public static final String PROVIDER_BUYPASS = "BUYPASS";

    // 인증서 갱신 임계값 (일)
    public static final int DEFAULT_RENEWAL_DAYS = 30;
    public static final int MIN_RENEWAL_DAYS = 7;
    public static final int MAX_RENEWAL_DAYS = 90;

    // 인증서 유효 기간 (일)
    public static final int CERTIFICATE_VALIDITY_DAYS = 90; // Let's Encrypt 기본값

    // 키 길이
    public static final int RSA_KEY_SIZE = 2048;
    public static final int RSA_KEY_SIZE_SECURE = 4096;
    public static final String KEY_ALGORITHM = "RSA";

    // 인증서 파일 확장자
    public static final String CERT_FILE_EXTENSION = ".crt";
    public static final String KEY_FILE_EXTENSION = ".key";
    public static final String CSR_FILE_EXTENSION = ".csr";
    public static final String PEM_FILE_EXTENSION = ".pem";
    public static final String P12_FILE_EXTENSION = ".p12";

    // PEM 헤더/푸터
    public static final String PEM_CERT_BEGIN = "-----BEGIN CERTIFICATE-----";
    public static final String PEM_CERT_END = "-----END CERTIFICATE-----";
    public static final String PEM_KEY_BEGIN = "-----BEGIN PRIVATE KEY-----";
    public static final String PEM_KEY_END = "-----END PRIVATE KEY-----";
    public static final String PEM_RSA_KEY_BEGIN = "-----BEGIN RSA PRIVATE KEY-----";
    public static final String PEM_RSA_KEY_END = "-----END RSA PRIVATE KEY-----";
}
package com.hwgi.autocert.certificate.acme.challenge;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ACME 챌린지 타입 열거형
 *
 * ACME 프로토콜에서 지원하는 도메인 소유권 검증 방식
 */
@Getter
@RequiredArgsConstructor
public enum ChallengeType {

    /**
     * HTTP-01 챌린지
     *
     * 웹서버의 /.well-known/acme-challenge/ 경로에 토큰 파일을 배치하여 도메인 소유권 증명
     *
     * 장점:
     * - 간단한 구현
     * - 포트 80만 열려있으면 가능
     *
     * 단점:
     * - 와일드카드 인증서 발급 불가
     * - 방화벽 뒤 서버는 어려움
     */
    HTTP_01("http-01"),

    /**
     * DNS-01 챌린지 (기본값)
     *
     * DNS TXT 레코드를 추가하여 도메인 소유권 증명
     *
     * 장점:
     * - 와일드카드 인증서 지원 (*.example.com)
     * - 내부 서버도 가능 (외부 접근 불필요)
     *
     * 단점:
     * - DNS API 연동 필요
     * - DNS 전파 시간 소요
     */
    DNS_01("dns-01"),

    /**
     * TLS-ALPN-01 챌린지 (선택적 구현)
     *
     * TLS ALPN 확장을 사용한 도메인 소유권 증명
     *
     * 장점:
     * - 포트 443만 사용
     * - HTTP 서버 불필요
     *
     * 단점:
     * - 구현 복잡도 높음
     * - 제한적인 사용 사례
     */
    TLS_ALPN_01("tls-alpn-01");

    /**
     * ACME 프로토콜에서 사용하는 챌린지 타입 문자열
     */
    private final String value;

    /**
     * 문자열로부터 ChallengeType 조회
     *
     * @param value 챌린지 타입 문자열 (http-01, dns-01, tls-alpn-01)
     * @return ChallengeType 열거형
     * @throws IllegalArgumentException 지원하지 않는 챌린지 타입
     */
    public static ChallengeType fromValue(String value) {
        if (value == null) {
            return DNS_01; // 기본값은 DNS-01
        }

        for (ChallengeType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
            "Unsupported challenge type: " + value +
            ". Supported types: http-01, dns-01, tls-alpn-01"
        );
    }

    /**
     * 기본 챌린지 타입 반환
     *
     * @return DNS-01 (와일드카드 지원 및 범용성 고려)
     */
    public static ChallengeType getDefault() {
        return DNS_01;
    }

    @Override
    public String toString() {
        return value;
    }
}

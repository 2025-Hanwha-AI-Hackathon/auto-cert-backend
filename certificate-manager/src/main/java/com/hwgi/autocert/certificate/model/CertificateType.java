package com.hwgi.autocert.certificate.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 인증서 타입 열거형
 *
 * 인증서의 검증 레벨과 발급 방식에 따른 구분
 */
@Getter
@RequiredArgsConstructor
public enum CertificateType {

    /**
     * DV (Domain Validated) 인증서
     *
     * - 도메인 소유권만 검증
     * - ACME 프로토콜 지원 (Let's Encrypt, ZeroSSL 등)
     * - 자동 발급 및 갱신 가능
     * - 발급 시간: 수분 이내
     * - 무료 또는 저렴한 비용
     * - 일반 웹사이트, API 서버 등에 적합
     */
    DV("DV", "Domain Validated", true),

    /**
     * OV (Organization Validated) 인증서
     *
     * - 도메인 소유권 + 조직 실재성 검증
     * - ACME 프로토콜 미지원 (수동 발급)
     * - CA(Certificate Authority)를 통한 수동 발급
     * - 발급 시간: 1-3일
     * - 유료 (연간 수십만원~)
     * - 기업 웹사이트, 전자상거래 등에 적합
     *
     * TODO: OV 인증서 수동 발급 프로세스 구현
     * - CSR 생성 및 제출
     * - 조직 검증 서류 제출
     * - CA 승인 대기
     * - 발급된 인증서 등록
     */
    OV("OV", "Organization Validated", false),

    /**
     * EV (Extended Validation) 인증서
     *
     * - 도메인 소유권 + 조직 실재성 + 법적 실체 엄격 검증
     * - ACME 프로토콜 미지원 (수동 발급)
     * - CA를 통한 엄격한 심사 후 발급
     * - 발급 시간: 1-2주
     * - 유료 (연간 수백만원~)
     * - 주소창에 회사명 표시 (일부 브라우저)
     * - 금융, 결제, 보안이 중요한 서비스에 적합
     *
     * TODO: EV 인증서 수동 발급 프로세스 구현
     * - CSR 생성 및 제출
     * - 법인 등록증, 사업자등록증 등 서류 제출
     * - 전화 인증 (CA가 직접 회사에 전화)
     * - CA 승인 대기
     * - 발급된 인증서 등록
     */
    EV("EV", "Extended Validation", false)
    ;


    /**
     * 인증서 타입 코드
     */
    private final String code;

    /**
     * 인증서 타입 설명
     */
    private final String description;

    /**
     * ACME 프로토콜 지원 여부
     *
     * true: 자동 발급/갱신 가능 (DV, WILDCARD)
     * false: 수동 프로세스 필요 (OV, EV)
     */
    private final boolean acmeSupported;

    /**
     * 코드로부터 CertificateType 조회
     *
     * @param code 인증서 타입 코드 (DV, OV, EV, WILDCARD)
     * @return CertificateType 열거형
     * @throws IllegalArgumentException 지원하지 않는 인증서 타입
     */
    public static CertificateType fromCode(String code) {
        if (code == null) {
            return DV; // 기본값은 DV
        }

        for (CertificateType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
            "Unsupported certificate type: " + code +
            ". Supported types: DV, OV, EV, WILDCARD"
        );
    }

    @Override
    public String toString() {
        return code;
    }
}

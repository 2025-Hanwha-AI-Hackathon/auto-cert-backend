package com.hwgi.autocert.certificate.config;

import com.hwgi.autocert.certificate.acme.challenge.ChallengeType;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * ACME 프로토콜 설정 프로퍼티
 */
@Getter
@ConfigurationProperties(prefix = "autocert.certificate.acme")
public class AcmeProperties {

    /**
     * ACME 서버 디렉토리 URL
     * - Let's Encrypt Production: https://acme-v02.api.letsencrypt.org/directory
     * - Let's Encrypt Staging: https://acme-staging-v02.api.letsencrypt.org/directory
     * - ZeroSSL: https://acme.zerossl.com/v2/DV90
     */
    private String directoryUrl;

    /**
     * ACME 계정 이메일 (필수)
     */
    private String accountEmail;

    /**
     * 키 알고리즘 (RSA, ECDSA)
     */
    private String keyAlgorithm;

    /**
     * 키 사이즈
     * - RSA: 2048, 4096
     * - ECDSA: 256, 384
     */
    private Integer keySize;

    /**
     * 서비스 약관 자동 동의 여부
     */
    private Boolean acceptTerms;

    /**
     * 챌린지 타임아웃 (초)
     */
    private Integer challengeTimeout;

    /**
     * 챌린지 폴링 간격 (밀리초)
     */
    private Long challengePollingInterval;

    /**
     * 인증서 발급 타임아웃 (초)
     */
    private Integer orderTimeout;

    /**
     * 인증서 발급 폴링 간격 (밀리초)
     */
    private Long orderPollingInterval;

    /**
     * 기본 챌린지 타입
     * - HTTP_01: 웹서버를 통한 검증 (단일 도메인)
     * - DNS_01: DNS TXT 레코드를 통한 검증 (와일드카드 지원, 기본값)
     */
    private ChallengeType defaultChallengeType;

    /**
     * HTTP-01 챌린지 웹루트 디렉토리
     */
    private String http01Webroot;

    /**
     * DNS-01 챌린지 프로바이더 (manual, route53, cloudflare)
     */
    private String dns01Provider;

    /**
     * DNS 전파 대기 타임아웃 (초)
     */
    private Integer dnsPropagationTimeout;

    @ConstructorBinding
    public AcmeProperties(String directoryUrl,
                          String accountEmail,
                          String keyAlgorithm,
                          Integer keySize,
                          Boolean acceptTerms,
                          Integer challengeTimeout,
                          Long challengePollingInterval,
                          Integer orderTimeout,
                          Long orderPollingInterval,
                          String defaultChallengeType,
                          String http01Webroot,
                          String dns01Provider,
                          Integer dnsPropagationTimeout) {
        this.directoryUrl = directoryUrl;
        this.accountEmail = accountEmail;
        this.keyAlgorithm = keyAlgorithm;
        this.keySize = keySize;
        this.acceptTerms = acceptTerms;
        this.challengeTimeout = challengeTimeout;
        this.challengePollingInterval = challengePollingInterval;
        this.orderTimeout = orderTimeout;
        this.orderPollingInterval = orderPollingInterval;
        this.defaultChallengeType = ChallengeType.fromValue(defaultChallengeType);
        this.http01Webroot = http01Webroot;
        this.dns01Provider = dns01Provider;
        this.dnsPropagationTimeout = dnsPropagationTimeout;
    }
}

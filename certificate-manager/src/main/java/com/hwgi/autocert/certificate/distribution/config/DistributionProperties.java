package com.hwgi.autocert.certificate.distribution.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 배포 서비스 설정
 */
@Configuration
@ConfigurationProperties(prefix = "autocert.distribution")
@Getter
@Setter
public class DistributionProperties {

    private Ssh ssh = new Ssh();

    @Getter
    @Setter
    public static class Ssh {
        /**
         * SSH 연결 타임아웃 (밀리초)
         */
        private int timeout = 30000;

        /**
         * 최대 재시도 횟수
         */
        private int maxRetries = 3;

        /**
         * 재시도 대기 시간 (밀리초)
         */
        private int retryDelay = 1000;

        /**
         * SSH 포트
         */
        private int port = 22;

        /**
         * 인증서 기본 배포 경로
         */
        private String defaultCertPath = "/etc/ssl/certs";

        /**
         * 개인키 기본 배포 경로
         */
        private String defaultKeyPath = "/etc/ssl/private";
    }
}

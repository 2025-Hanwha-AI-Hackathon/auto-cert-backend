package com.hwgi.autocert.api;

import com.hwgi.autocert.certificate.config.AcmeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-Cert Main Application
 *
 * SSL/TLS 인증서 자동화 관리 시스템의 메인 애플리케이션 클래스
 */
@SpringBootApplication(scanBasePackages = {
    "com.hwgi.autocert.api",
    "com.hwgi.autocert.domain",
    "com.hwgi.autocert.certificate",
    "com.hwgi.autocert.common"
})
@EnableConfigurationProperties(AcmeProperties.class)
@EnableJpaRepositories(basePackages = "com.hwgi.autocert.domain.repository")
@EntityScan(basePackages = "com.hwgi.autocert.domain.model")
@EnableScheduling
public class AutoCertApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoCertApplication.class, args);
    }
}

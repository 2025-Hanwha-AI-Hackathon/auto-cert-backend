package com.hwgi.autocert.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 이메일 알림 설정
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.email")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "app.email",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class EmailProperties {
    
    /**
     * 이메일 수신자 목록
     */
    private List<String> recipients = Arrays.asList(
        "x2xgudwls@hanwha.com",
        "cheal3@hanwha.com",
        "sjhwang@hanwha.com"
    );
    
    /**
     * 발신자 이메일 주소
     */
    private String from = "noreply@autocert.com";
    
    /**
     * 발신자 이름
     */
    private String fromName = "AutoCert System";
    
    /**
     * 이메일 발송 활성화 여부
     */
    private boolean enabled = false;
}

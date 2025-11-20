package com.hwgi.autocert.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI Assistant 설정 프로퍼티
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.assistant")
public class AiProperties {
    
    /**
     * OpenAI API Key
     */
    private String apiKey;
    
    /**
     * 모델 이름 (gpt-4, gpt-3.5-turbo 등)
     */
    private String modelName = "gpt-3.5-turbo";
    
    /**
     * Temperature (0.0 ~ 1.0)
     */
    private Double temperature = 0.7;
    
    /**
     * Max tokens
     */
    private Integer maxTokens = 1000;
    
    /**
     * 채팅 히스토리 최대 저장 개수
     */
    private Integer maxHistorySize = 10;
}


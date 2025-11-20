package com.hwgi.autocert.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    @NotBlank(message = "메시지는 필수입니다")
    private String message;
    
    /**
     * 세션 ID (선택, 없으면 "default" 사용)
     */
    private String sessionId;
}


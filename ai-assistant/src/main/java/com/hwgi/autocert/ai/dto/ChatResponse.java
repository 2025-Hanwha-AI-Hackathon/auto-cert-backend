package com.hwgi.autocert.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    private String message;
    private String role;
    private LocalDateTime timestamp;
    private String sessionId;
    private Boolean success;
}


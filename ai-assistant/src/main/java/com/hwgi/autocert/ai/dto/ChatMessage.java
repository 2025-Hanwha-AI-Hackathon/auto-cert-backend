package com.hwgi.autocert.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    private String role;  // "user" or "assistant"
    private String content;
    private LocalDateTime timestamp;
}


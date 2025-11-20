package com.hwgi.autocert.api.controller;

import com.hwgi.autocert.ai.dto.ChatMessage;
import com.hwgi.autocert.ai.dto.ChatRequest;
import com.hwgi.autocert.ai.dto.ChatResponse;
import com.hwgi.autocert.ai.service.AiAssistantService;
import com.hwgi.autocert.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 채팅 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI 채팅 API")
public class ChatController {

    private final AiAssistantService aiAssistantService;

    @Operation(summary = "채팅 메시지 전송", description = "AI 어시스턴트에게 메시지를 보내고 응답을 받습니다")
    @PostMapping
    public ApiResponse<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat message: sessionId={}, message={}", 
                request.getSessionId(), request.getMessage());
        
        ChatResponse response = aiAssistantService.chat(request);
        
        if (response.getSuccess()) {
            return ApiResponse.success(response, "메시지 처리 완료");
        } else {
            return ApiResponse.success(response, "메시지 처리 중 오류 발생");
        }
    }

    @Operation(summary = "채팅 히스토리 조회", description = "특정 세션의 채팅 히스토리를 조회합니다")
    @GetMapping("/history/{sessionId}")
    public ApiResponse<List<ChatMessage>> getChatHistory(@PathVariable String sessionId) {
        log.info("Get chat history for session: {}", sessionId);
        
        List<ChatMessage> history = aiAssistantService.getChatHistory(sessionId);
        return ApiResponse.success(history, "채팅 히스토리 조회 완료");
    }

    @Operation(summary = "채팅 히스토리 삭제", description = "특정 세션의 채팅 히스토리를 삭제합니다")
    @DeleteMapping("/history/{sessionId}")
    public ApiResponse<Void> clearChatHistory(@PathVariable String sessionId) {
        log.info("Clear chat history for session: {}", sessionId);
        
        aiAssistantService.clearChatHistory(sessionId);
        return ApiResponse.success(null, "채팅 히스토리가 삭제되었습니다");
    }
}


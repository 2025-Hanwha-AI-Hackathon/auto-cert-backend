package com.hwgi.autocert.ai.service;

import com.hwgi.autocert.ai.config.AiProperties;
import com.hwgi.autocert.ai.dto.ChatMessage;
import com.hwgi.autocert.ai.dto.ChatRequest;
import com.hwgi.autocert.ai.dto.ChatResponse;
import com.hwgi.autocert.ai.tool.CertificateTools;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Assistant 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final ChatLanguageModel chatLanguageModel;
    private final AiProperties aiProperties;
    private final CertificateTools certificateTools;
    
    // 세션별 채팅 히스토리 저장 (간단한 구현, 실제로는 Redis 권장)
    private final Map<String, List<ChatMessage>> chatHistories = new ConcurrentHashMap<>();
    
    // 세션별 Assistant 인스턴스 저장 (각 세션마다 독립적인 ChatMemory 유지)
    private final Map<String, Assistant> assistants = new ConcurrentHashMap<>();

    /**
     * 세션별 Assistant 가져오기 또는 생성
     * 각 세션마다 독립적인 ChatMemory를 가진 Assistant 인스턴스 유지
     */
    private Assistant getOrCreateAssistant(String sessionId) {
        return assistants.computeIfAbsent(sessionId, id -> {
            log.info("Creating new Assistant for session: {}", id);
            
            // 세션별 독립적인 ChatMemory 생성
            ChatMemory sessionMemory = dev.langchain4j.memory.chat.MessageWindowChatMemory
                    .withMaxMessages(aiProperties.getMaxHistorySize());
            
            return AiServices.builder(Assistant.class)
                    .chatLanguageModel(chatLanguageModel)
                    .chatMemory(sessionMemory)  // ⭐ 세션별 독립 메모리
                    .tools(certificateTools)
                    .build();
        });
    }

    /**
     * 채팅 메시지 처리
     */
    public ChatResponse chat(ChatRequest request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "default";
        String userMessage = request.getMessage();
        
        log.info("Processing chat message: sessionId={}, message={}", sessionId, userMessage);
        
        try {
            // 히스토리에 사용자 메시지 저장
            saveChatHistory(sessionId, "user", userMessage);
            
            // 세션별 Assistant 가져오기 (독립적인 ChatMemory 유지)
            Assistant sessionAssistant = getOrCreateAssistant(sessionId);
            
            // AI Assistant 호출
            String aiResponse = sessionAssistant.chat(userMessage);
            
            // 히스토리에 AI 응답 저장
            saveChatHistory(sessionId, "assistant", aiResponse);
            
            log.info("AI response generated successfully for session: {}", sessionId);
            
            return ChatResponse.builder()
                    .message(aiResponse)
                    .role("assistant")
                    .timestamp(LocalDateTime.now())
                    .sessionId(sessionId)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            
            String errorMessage = "죄송합니다. 요청을 처리하는 중 오류가 발생했습니다.";
            
            // 히스토리에 에러 응답 저장
            saveChatHistory(sessionId, "assistant", errorMessage);
            
            return ChatResponse.builder()
                    .message(errorMessage)
                    .role("assistant")
                    .timestamp(LocalDateTime.now())
                    .sessionId(sessionId)
                    .success(false)
                    .build();
        }
    }

    /**
     * 채팅 히스토리 조회
     */
    public List<ChatMessage> getChatHistory(String sessionId) {
        return chatHistories.getOrDefault(sessionId, new ArrayList<>());
    }

    /**
     * 채팅 히스토리 클리어
     */
    public void clearChatHistory(String sessionId) {
        log.info("Clearing chat history for session: {}", sessionId);
        chatHistories.remove(sessionId);
    }

    /**
     * 채팅 히스토리 저장
     */
    private void saveChatHistory(String sessionId, String role, String content) {
        chatHistories.computeIfAbsent(sessionId, k -> new ArrayList<>())
                .add(ChatMessage.builder()
                        .role(role)
                        .content(content)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * AI Assistant Interface
     * SystemMessage로 역할 정의
     */
    interface Assistant {
        @SystemMessage("""
                You are an AI assistant for an SSL/TLS certificate management system.
                Your role is to help users manage their certificates efficiently.
                
                You can help users with:
                
                📄 CERTIFICATE MANAGEMENT:
                - Creating new certificates (등록, 생성, 발급)
                - Viewing, filtering, and searching certificates
                - Renewing certificates (갱신)
                - Deleting certificates (삭제)
                - Viewing certificate statistics and summaries
                - Finding certificates that are expiring soon
                
                Available tools:
                - getAllCertificates: Show all certificates
                - getCertificatesByStatus: Filter by status (ACTIVE/유효, EXPIRING_SOON/곧 만료, EXPIRED/만료됨)
                - searchCertificateByDomain: Search by domain name
                - getCertificateById: Get certificate details by ID
                - createCertificate: Create new certificate
                  * Required: domain (example.com)
                  * Optional: challengeType (default: DNS_01), admin (관리자), alertDaysBeforeExpiry (default: 7)
                - renewCertificate: Renew certificate by ID
                - deleteCertificate: Delete certificate by ID (ask for confirmation!)
                - getCertificatesExpiringSoon: Show certificates expiring within 30 days
                - getCertificateStatistics: Show statistics (total, active, expiring, expired)
                
                Important guidelines:
                - Always respond in Korean (한국어)
                - Be helpful, concise, and professional
                - When filtering, map natural language to status: "유효한" → ACTIVE, "만료될/곧 만료" → EXPIRING_SOON, "만료된" → EXPIRED
                - Before renewing or deleting, search for the certificate first to get its ID
                - When creating certificates, ask for required information if missing
                - For deletions, confirm with user before calling delete tools
                - Use appropriate emojis (✅ ⚠️ ❌ 📄 📊 🔄 🗑️)
                - If user's intent is unclear, ask for clarification
                - After performing actions, confirm what was done
                
                Example interactions:
                - "example.com 인증서 등록해줘" → createCertificate(domain="example.com")
                - "10.10.10.10 IP에 example.com 인증서 등록" → createCertificate(domain="example.com") (IP is server info, not needed for certificate)
                - "유효한 인증서 보여줘" → getCertificatesByStatus("ACTIVE")
                - "example.com 갱신해줘" → searchCertificateByDomain("example.com") → renewCertificate(id)
                - "곧 만료될 인증서" → getCertificatesExpiringSoon()
                - "통계 보여줘" → getCertificateStatistics()
                - "example.com 삭제" → searchCertificateByDomain → Ask confirmation → deleteCertificate(id)
                
                Note: Server management is available through the web UI, not through chat commands.
                """)
        String chat(@UserMessage String message);
    }
}


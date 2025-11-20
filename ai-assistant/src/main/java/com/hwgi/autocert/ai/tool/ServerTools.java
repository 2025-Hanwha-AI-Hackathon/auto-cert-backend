package com.hwgi.autocert.ai.tool;

import com.hwgi.autocert.common.constants.WebServerType;
import com.hwgi.autocert.domain.model.Server;
import com.hwgi.autocert.server.service.ServerService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 서버 관리 AI Tools
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServerTools {

    private final ServerService serverService;

    @Tool("Create a new server for certificate deployment. Use this ONLY after user explicitly confirms creation. IMPORTANT: This tool should be called ONLY after showing what will be created and getting user confirmation. Required: IP address, username, password. Optional: server name, port (default: 22), web server type (default: nginx), deploy path (default: /etc/nginx/ssl), description.")
    public String createServer(
            String ipAddress,
            String username,
            String password,
            String serverName,
            Integer port,
            String webServerType,
            String deployPath,
            String description) {
        log.info("Tool called: createServer with ipAddress={}, username={}, serverName={}, port={}, webServerType={}, deployPath={}",
                ipAddress, username, serverName, port, webServerType, deployPath);

        try {
            // 기본값 설정
            String finalServerName = serverName != null ? serverName : "Server-" + ipAddress;
            Integer finalPort = port != null ? port : 22;
            WebServerType finalWebServerType = webServerType != null ? 
                    WebServerType.fromCode(webServerType) : WebServerType.NGINX;
            String finalDeployPath = deployPath != null ? deployPath : "/etc/nginx/ssl";

            // 서버 생성
            Server server = serverService.create(
                    finalServerName,
                    ipAddress,
                    finalPort,
                    finalWebServerType,
                    description,
                    username,
                    password,
                    finalDeployPath
            );

            return String.format(
                    "✅ 새 서버가 성공적으로 등록되었습니다!\n\n" +
                    "🖥️ 서버 정보:\n" +
                    "- ID: %d\n" +
                    "- 이름: %s\n" +
                    "- IP 주소: %s\n" +
                    "- 포트: %d\n" +
                    "- 웹서버 타입: %s\n" +
                    "- 사용자명: %s\n" +
                    "- 배포 경로: %s\n" +
                    "%s" +
                    "- 등록일: %s\n\n" +
                    "이제 이 서버에 인증서를 배포할 수 있습니다.",
                    server.getId(),
                    server.getName(),
                    server.getIpAddress(),
                    server.getPort(),
                    server.getWebServerType().getCode(),
                    server.getUsername(),
                    server.getDeployPath(),
                    description != null ? "- 설명: " + description + "\n" : "",
                    server.getCreatedAt() != null ? server.getCreatedAt().toLocalDate().toString() : "방금"
            );
        } catch (IllegalArgumentException e) {
            log.error("Error creating server - invalid argument", e);
            String errorMsg = e.getMessage();

            if (errorMsg.contains("이미 존재하는 IP 주소")) {
                return "❌ 서버 등록 실패\n\n" +
                       "🔍 **실패 원인**\n" +
                       "이 IP 주소(" + ipAddress + ")는 이미 등록되어 있습니다.\n" +
                       "중복 등록은 불가능합니다.\n\n" +
                       "💡 **해결 방법**\n" +
                       "1️⃣ 기존 서버를 확인하시겠습니까?\n" +
                       "   → \"서버 목록 보여줘\" 라고 말씀해주세요\n\n" +
                       "2️⃣ 다른 IP 주소로 등록\n" +
                       "   → 다른 서버의 IP 주소를 알려주세요\n\n" +
                       "어떻게 하시겠습니까?";
            } else if (errorMsg.contains("비밀번호")) {
                return "❌ 서버 등록 실패\n\n" +
                       "🔍 **실패 원인**\n" +
                       "SSH 비밀번호가 제공되지 않았습니다.\n" +
                       "서버 접속을 위해서는 비밀번호가 필수입니다.\n\n" +
                       "💡 **해결 방법**\n" +
                       "SSH 비밀번호를 포함하여 다시 입력해주세요.\n\n" +
                       "📝 **다음 단계**\n" +
                       "서버 정보를 다시 알려주세요. (IP 주소, 사용자명, 비밀번호 포함)";
            } else {
                return "❌ 서버 등록 실패\n\n" +
                       "🔍 **실패 원인**\n" +
                       errorMsg + "\n\n" +
                       "💡 **해결 방법**\n" +
                       "입력하신 정보를 다시 확인해주세요.\n" +
                       "- IP 주소 형식이 올바른가요? (예: 192.168.1.100)\n" +
                       "- 포트 번호가 올바른가요? (기본값: 22)\n" +
                       "- 웹서버 타입이 올바른가요? (nginx, apache, tomcat, iis)\n\n" +
                       "📝 **다시 시도**\n" +
                       "올바른 정보로 다시 알려주세요.";
            }
        } catch (Exception e) {
            log.error("Error creating server", e);
            String errorMsg = e.getMessage();
            String causeMsg = e.getCause() != null ? e.getCause().getMessage() : "";

            return "❌ 서버 등록 중 예상치 못한 오류가 발생했습니다\n\n" +
                   "📋 **에러 상세 정보**\n" +
                   (errorMsg != null ? errorMsg : "알 수 없는 오류") + "\n" +
                   (causeMsg != null && !causeMsg.isEmpty() ? "근본 원인: " + causeMsg + "\n" : "") + "\n" +
                   "💡 **해결 방법**\n" +
                   "1️⃣ 입력한 정보가 모두 올바른지 확인\n" +
                   "2️⃣ 서버가 정상 작동 중인지 확인\n" +
                   "3️⃣ 잠시 후 다시 시도\n" +
                   "4️⃣ 문제가 계속되면 관리자에게 문의\n\n" +
                   "📝 **다음 단계**\n" +
                   "다른 서버로 다시 시도하시겠습니까?";
        }
    }
}


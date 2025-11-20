package com.hwgi.autocert.ai.tool;

import com.hwgi.autocert.certificate.service.CertificateService;
import com.hwgi.autocert.domain.model.Certificate;
import com.hwgi.autocert.domain.model.Server;
import com.hwgi.autocert.domain.repository.ServerRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 인증서 관리 Tool (Function Calling)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateTools {

    private final CertificateService certificateService;
    private final ServerRepository serverRepository;

    @Tool("Get all certificates list. Use this when user asks to see all certificates or certificate list.")
    public String getAllCertificates() {
        log.info("Tool called: getAllCertificates");
        
        try {
            Page<Certificate> certificates = certificateService.findAll(PageRequest.of(0, 100));
            
            if (certificates.isEmpty()) {
                return "현재 등록된 인증서가 없습니다.";
            }
            
            return formatCertificateList(certificates.getContent());
        } catch (Exception e) {
            log.error("Error getting all certificates", e);
            return "인증서 목록 조회 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    @Tool("Get certificates by status. Status can be: ACTIVE (valid/normal), EXPIRING_SOON (will expire soon), EXPIRED (already expired), PENDING, RENEWING, or FAILED. Use this when user wants to filter certificates by their status.")
    public String getCertificatesByStatus(String status) {
        log.info("Tool called: getCertificatesByStatus with status={}", status);
        
        try {
            Page<Certificate> certificates = certificateService.findAll(PageRequest.of(0, 100));
            List<Certificate> filtered = certificates.getContent().stream()
                    .filter(cert -> cert.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
            
            if (filtered.isEmpty()) {
                return String.format("%s 상태의 인증서가 없습니다.", translateStatus(status));
            }
            
            return formatCertificateList(filtered);
        } catch (Exception e) {
            log.error("Error getting certificates by status", e);
            return "인증서 조회 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    @Tool("Search for certificates by domain name. Use this when user mentions a specific domain or wants to find a certificate by name.")
    public String searchCertificateByDomain(String domain) {
        log.info("Tool called: searchCertificateByDomain with domain={}", domain);
        
        try {
            Page<Certificate> certificates = certificateService.findAll(PageRequest.of(0, 100));
            List<Certificate> matched = certificates.getContent().stream()
                    .filter(cert -> cert.getDomain().toLowerCase().contains(domain.toLowerCase()))
                    .collect(Collectors.toList());
            
            if (matched.isEmpty()) {
                return String.format("'%s' 도메인을 가진 인증서를 찾을 수 없습니다.", domain);
            }
            
            if (matched.size() == 1) {
                return formatCertificateDetail(matched.get(0));
            }
            
            return formatCertificateList(matched);
        } catch (Exception e) {
            log.error("Error searching certificate", e);
            return "인증서 검색 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    @Tool("Renew a certificate by ID. Use this ONLY after user explicitly confirms renewal. IMPORTANT: Before calling this, you MUST: 1) Search certificate by domain using searchCertificateByDomain, 2) Show certificate info to user, 3) Ask for confirmation, 4) ONLY if user confirms, call this tool with the certificate ID.")
    public String renewCertificate(Long certificateId) {
        log.info("Tool called: renewCertificate with id={}", certificateId);
        
        try {
            Certificate renewed = certificateService.renew(certificateId, null);
            return String.format(
                "✅ 인증서 갱신 성공!\n\n" +
                "📄 인증서 정보:\n" +
                "- 도메인: %s\n" +
                "- 상태: %s\n" +
                "- 새 만료일: %s\n\n" +
                "인증서가 성공적으로 갱신되었습니다.",
                renewed.getDomain(),
                translateStatus(renewed.getStatus().name()),
                renewed.getExpiresAt() != null ? renewed.getExpiresAt().toLocalDate().toString() : "N/A"
            );
        } catch (Exception e) {
            log.error("Error renewing certificate", e);
            String errorMsg = e.getMessage();
            String causeMsg = e.getCause() != null ? e.getCause().getMessage() : "";
            
            // 에러 원인 분석
            String detailedCause = "";
            String solution = "";
            
            if (errorMsg != null && (errorMsg.contains("not found") || errorMsg.contains("찾을 수 없") || errorMsg.contains("존재하지 않"))) {
                detailedCause = "인증서를 찾을 수 없습니다.\n해당 ID의 인증서가 존재하지 않거나 이미 삭제되었습니다.";
                solution = "1️⃣ 인증서 목록을 다시 확인해주세요\n" +
                          "   → \"인증서 목록 보여줘\" 라고 말씀해주세요\n\n" +
                          "2️⃣ 도메인으로 다시 검색해주세요\n" +
                          "   → \"[도메인] 검색해줘\" 라고 말씀해주세요";
            } else if (errorMsg != null && (errorMsg.contains("ACME") || errorMsg.contains("acme"))) {
                detailedCause = "ACME 프로토콜 오류입니다.\nLet's Encrypt 서버와의 통신에 문제가 있습니다.";
                solution = "1️⃣ 도메인이 인터넷에서 접근 가능한지 확인\n" +
                          "2️⃣ DNS 설정이 올바른지 확인\n" +
                          "3️⃣ Let's Encrypt Rate Limit 초과 여부 확인\n" +
                          "   → 같은 도메인을 짧은 시간에 여러 번 갱신하면 제한됩니다\n" +
                          "4️⃣ 30분~1시간 후 다시 시도";
            } else if (errorMsg != null && (errorMsg.contains("DNS") || errorMsg.contains("dns") || errorMsg.contains("validation"))) {
                detailedCause = "DNS 인증(Challenge) 실패입니다.\nCloudflare DNS 설정 또는 도메인 소유권 인증에 실패했습니다.";
                solution = "1️⃣ Cloudflare에 해당 도메인이 등록되어 있는지 확인\n" +
                          "2️⃣ Cloudflare API 토큰 권한 확인:\n" +
                          "   - Zone.DNS 권한이 있어야 합니다\n" +
                          "   - API 토큰이 만료되지 않았는지 확인\n" +
                          "3️⃣ 도메인의 네임서버가 Cloudflare로 설정되어 있는지 확인\n" +
                          "4️⃣ Cloudflare 대시보드에서 DNS 레코드 확인";
            } else if (errorMsg != null && (errorMsg.contains("expired") || errorMsg.contains("만료"))) {
                detailedCause = "인증서가 이미 만료되었습니다.";
                solution = "1️⃣ 만료된 인증서는 정상적으로 갱신 가능합니다\n" +
                          "2️⃣ DNS 설정과 Cloudflare 연동을 확인해주세요\n" +
                          "3️⃣ 다시 시도해주세요";
            } else if (errorMsg != null && (errorMsg.contains("permission") || errorMsg.contains("권한") || errorMsg.contains("access denied"))) {
                detailedCause = "권한 문제입니다.\nAPI 토큰 또는 서버 접근 권한이 부족합니다.";
                solution = "1️⃣ Cloudflare API 토큰 권한 확인\n" +
                          "2️⃣ 서버 SSH 접근 권한 확인\n" +
                          "3️⃣ 관리자 계정으로 다시 시도";
            } else {
                detailedCause = "예상치 못한 오류가 발생했습니다.\n" + (errorMsg != null ? errorMsg : "알 수 없는 오류");
                solution = "1️⃣ 인증서 상태 확인:\n" +
                          "   → \"[도메인] 인증서 정보 보여줘\"\n\n" +
                          "2️⃣ 현재 상태가 갱신 가능한지 확인\n" +
                          "   - ACTIVE, EXPIRING_SOON, EXPIRED 상태는 갱신 가능\n" +
                          "   - PENDING, RENEWING 상태는 잠시 기다렸다가 시도\n\n" +
                          "3️⃣ 잠시 후 (5~10분) 다시 시도\n\n" +
                          "4️⃣ 문제가 계속되면 관리자에게 문의";
            }
            
            return "❌ 인증서 갱신 실패\n\n" +
                   "🔍 **실패 원인**\n" +
                   detailedCause + "\n\n" +
                   "📋 **에러 상세 정보**\n" +
                   (errorMsg != null ? errorMsg : "에러 메시지 없음") + "\n" +
                   (causeMsg != null && !causeMsg.isEmpty() ? "근본 원인: " + causeMsg + "\n" : "") + "\n" +
                   "💡 **해결 방법**\n" +
                   solution + "\n\n" +
                   "📝 **다음 단계**\n" +
                   "위 해결 방법을 시도하신 후 다시 갱신을 요청해주세요.\n" +
                   "다른 도메인을 갱신하시겠습니까?";
        }
    }

    @Tool("Get certificate statistics including total, active, expiring soon, and expired counts. Use this when user asks about statistics, status summary, or overview.")
    public String getCertificateStatistics() {
        log.info("Tool called: getCertificateStatistics");
        
        try {
            Page<Certificate> allCerts = certificateService.findAll(PageRequest.of(0, 100));
            long total = allCerts.getTotalElements();
            long active = allCerts.getContent().stream()
                    .filter(c -> "ACTIVE".equals(c.getStatus().name()))
                    .count();
            long expiringSoon = allCerts.getContent().stream()
                    .filter(c -> "EXPIRING_SOON".equals(c.getStatus().name()))
                    .count();
            long expired = allCerts.getContent().stream()
                    .filter(c -> "EXPIRED".equals(c.getStatus().name()))
                    .count();
            long pending = allCerts.getContent().stream()
                    .filter(c -> "PENDING".equals(c.getStatus().name()) || "RENEWING".equals(c.getStatus().name()))
                    .count();
            
            return String.format(
                "📊 인증서 통계\n\n" +
                "전체: %d개\n" +
                "✅ 유효: %d개\n" +
                "⚠️ 곧 만료: %d개\n" +
                "❌ 만료됨: %d개\n" +
                "🔄 처리 중: %d개",
                total, active, expiringSoon, expired, pending
            );
        } catch (Exception e) {
            log.error("Error getting statistics", e);
            return "통계 조회 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    @Tool("Create a new certificate for a domain. Use this ONLY after user explicitly confirms creation. IMPORTANT: This tool should be called ONLY after showing what will be created and getting user confirmation. Required: domain name. Optional: challenge type (default: dns-01), admin name, alert days.")
    public String createCertificate(
            String domain,
            String challengeType,
            String admin,
            Integer alertDaysBeforeExpiry) {
        log.info("Tool called: createCertificate with domain={}, challengeType={}, admin={}, alertDays={}", 
                 domain, challengeType, admin, alertDaysBeforeExpiry);
        
        try {
            // 첫 번째 서버 조회
            Server server = serverRepository.findAll(PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("등록된 서버가 없습니다. 먼저 서버를 등록해주세요."));
            
            Certificate cert = certificateService.create(
                server.getId(),
                domain,
                challengeType != null ? challengeType : "dns-01",
                admin,
                alertDaysBeforeExpiry != null ? alertDaysBeforeExpiry : 7,
                false  // autoDeploy 기본값: false
            );
            
            return String.format(
                "✅ 새 인증서가 성공적으로 등록되었습니다!\n\n" +
                "📄 인증서 정보:\n" +
                "- ID: %d\n" +
                "- 도메인: %s\n" +
                "- 챌린지 타입: %s\n" +
                "- 상태: %s\n" +
                "- 관리자: %s\n" +
                "- 알림 설정: 만료 %d일 전\n" +
                "- 생성일: %s\n\n" +
                "인증서가 성공적으로 생성되었습니다. ACME 프로토콜을 통해 자동으로 발급됩니다.",
                cert.getId(),
                cert.getDomain(),
                challengeType != null ? challengeType : "dns-01",
                translateStatus(cert.getStatus().name()),
                admin != null ? admin : "미지정",
                alertDaysBeforeExpiry != null ? alertDaysBeforeExpiry : 7,
                cert.getCreatedAt() != null ? cert.getCreatedAt().toLocalDate().toString() : "방금"
            );
        } catch (IllegalStateException e) {
            // 서버가 등록되지 않은 경우
            log.error("Error creating certificate - no server registered", e);
            return "❌ 인증서 생성 실패\n\n" +
                   "🔍 **실패 원인**\n" +
                   "등록된 서버가 없습니다.\n" +
                   "인증서를 생성하려면 먼저 배포할 서버 정보가 필요합니다.\n\n" +
                   "💡 **해결 방법 (필수)**\n" +
                   "다음 방법 중 하나를 선택해주세요:\n\n" +
                   "1️⃣ **채팅으로 서버 등록하기 (추천)**\n" +
                   "   → '서버 등록하고 싶어' 라고 말씀해주세요\n" +
                   "   → 필요한 정보를 순서대로 안내해드립니다\n" +
                   "   → 등록 완료 후 다시 인증서를 생성하시면 됩니다\n\n" +
                   "2️⃣ **웹 UI에서 직접 등록하기**\n" +
                   "   → 웹 페이지 왼쪽 메뉴에서 '서버 관리' 클릭\n" +
                   "   → '서버 추가' 버튼 클릭\n" +
                   "   → 서버 정보 입력 (IP, 포트, 사용자명, 비밀번호)\n" +
                   "   → 저장 후 이 대화창으로 돌아오세요\n\n" +
                   "어떤 방법을 선택하시겠습니까?";
        } catch (IllegalArgumentException e) {
            // 도메인 형식이나 기타 인자 오류
            log.error("Error creating certificate - invalid argument", e);
            String errorMsg = e.getMessage().toLowerCase();
            
            if (errorMsg.contains("domain") || errorMsg.contains("도메인")) {
                return "❌ 인증서 생성 실패\n\n" +
                       "🔍 **실패 원인**\n" +
                       "입력하신 도메인 형식이 올바르지 않습니다.\n" +
                       "에러 상세: " + e.getMessage() + "\n\n" +
                       "💡 **올바른 도메인 형식**\n" +
                       "✅ 올바른 예시:\n" +
                       "  • example.com\n" +
                       "  • www.example.com\n" +
                       "  • subdomain.example.com\n" +
                       "  • example.co.kr\n" +
                       "  • api.myservice.io\n\n" +
                       "❌ 잘못된 예시:\n" +
                       "  • example (TLD 없음)\n" +
                       "  • http://example.com (프로토콜 포함)\n" +
                       "  • example.com/ (슬래시 포함)\n" +
                       "  • example .com (공백 포함)\n\n" +
                       "📝 **다시 입력해주세요**\n" +
                       "올바른 형식의 도메인 이름을 알려주세요.\n" +
                       "(예: example.com)";
            } else if (errorMsg.contains("이미 존재") || errorMsg.contains("duplicate") || errorMsg.contains("already exists")) {
                return "⚠️ 인증서 등록 불가\n\n" +
                       "🔍 **실패 원인**\n" +
                       "이 도메인은 이미 인증서가 등록되어 있습니다.\n" +
                       "중복 등록은 불가능합니다.\n\n" +
                       "💡 **해결 방법**\n" +
                       "다음 중 하나를 선택해주세요:\n\n" +
                       "1️⃣ **기존 인증서 갱신**\n" +
                       "   → \"[도메인] 인증서를 갱신해줘\" 라고 말씀해주세요\n\n" +
                       "2️⃣ **다른 도메인 등록**\n" +
                       "   → 등록하실 새로운 도메인 이름을 알려주세요\n\n" +
                       "3️⃣ **기존 인증서 삭제 후 재등록**\n" +
                       "   → \"[도메인] 인증서를 삭제해줘\" 라고 먼저 말씀해주세요\n\n" +
                       "어떻게 하시겠습니까?";
            } else {
                return "❌ 인증서 생성 실패\n\n" +
                       "🔍 **실패 원인**\n" +
                       e.getMessage() + "\n\n" +
                       "💡 **해결 방법**\n" +
                       "입력하신 정보를 다시 확인해주세요.\n" +
                       "- 도메인 형식이 올바른가요? (예: example.com)\n" +
                       "- 특수문자가 포함되어 있지 않나요?\n\n" +
                       "📝 **다시 시도**\n" +
                       "올바른 도메인 이름을 다시 알려주세요.";
            }
        } catch (Exception e) {
            // 기타 예외 - 더 구체적으로 분석
            log.error("Error creating certificate", e);
            String errorMsg = e.getMessage();
            String causeMsg = e.getCause() != null ? e.getCause().getMessage() : "";
            
            // 에러 메시지 분석하여 구체적인 원인 파악
            String detailedCause = "";
            String solution = "";
            
            // Cloudflare Zone 관련 에러 (가장 구체적으로 먼저 체크)
            if ((errorMsg != null && (errorMsg.contains("Zone not found") || errorMsg.contains("Cloudflare zone ID"))) ||
                (causeMsg != null && (causeMsg.contains("Zone not found") || causeMsg.contains("Cloudflare zone ID")))) {
                String zoneDomain = domain;
                if (domain.split("\\.").length > 2) {
                    String[] parts = domain.split("\\.");
                    zoneDomain = parts[parts.length - 2] + "." + parts[parts.length - 1];
                }
                
                return "❌ 인증서 생성 실패\n\n" +
                       "🔍 **실패 원인: Cloudflare Zone을 찾을 수 없습니다**\n" +
                       "'" + domain + "' 도메인의 Cloudflare Zone이 존재하지 않거나 접근할 수 없습니다.\n" +
                       "(Zone 도메인: " + zoneDomain + ")\n\n" +
                       "💡 **해결 방법 (다음 단계를 순서대로 확인하세요)**\n\n" +
                       "**1️⃣ Cloudflare에 도메인 등록 확인**\n" +
                       "   ① Cloudflare 대시보드 접속: https://dash.cloudflare.com/\n" +
                       "   ② 좌측 메뉴에서 'Websites' 클릭\n" +
                       "   ③ '" + zoneDomain + "' 도메인이 목록에 있는지 확인\n" +
                       "   ④ 도메인이 **없다면**: 'Add a Site' 버튼으로 도메인 추가 필요\n" +
                       "   ⑤ 도메인이 **있다면**: 상태가 'Active'인지 확인\n\n" +
                       "**2️⃣ 도메인 상태 확인**\n" +
                       "   • 상태가 'Pending'이라면:\n" +
                       "     → 도메인 등록대행사에서 네임서버를 Cloudflare로 변경해야 합니다\n" +
                       "     → Cloudflare가 제공한 네임서버 주소로 설정\n" +
                       "     → 예: ns1.cloudflare.com, ns2.cloudflare.com\n" +
                       "   • 상태가 'Active'라면:\n" +
                       "     → 3단계 API 토큰 확인으로 이동\n\n" +
                       "**3️⃣ API 토큰 권한 확인**\n" +
                       "   ① Cloudflare 대시보드 → 프로필 → 'API Tokens'\n" +
                       "   ② 현재 사용 중인 토큰 찾기\n" +
                       "   ③ 'Edit' 클릭하여 다음 확인:\n" +
                       "      • Zone Resources: 'All zones' 또는 '" + zoneDomain + "' 포함\n" +
                       "      • Permissions: 'Zone:Read', 'DNS:Edit' 권한 보유\n\n" +
                       "**4️⃣ 도메인 추가 방법 (등록되지 않은 경우)**\n" +
                       "   ① Cloudflare 대시보드에서 'Add a Site' 클릭\n" +
                       "   ② 도메인 입력: " + zoneDomain + "\n" +
                       "   ③ 플랜 선택: Free 플랜 선택 가능\n" +
                       "   ④ DNS 레코드 스캔 대기\n" +
                       "   ⑤ 네임서버 변경:\n" +
                       "      • 도메인 등록대행사 접속\n" +
                       "      • 네임서버를 Cloudflare 제공 주소로 변경\n" +
                       "   ⑥ DNS 전파 대기 (보통 1~24시간)\n\n" +
                       "📝 **문제 해결 후**\n" +
                       "위 단계를 완료하셨다면 다시 시도해주세요.\n" +
                       "어떤 도메인을 등록하시겠습니까?";
            } else if (errorMsg != null && (errorMsg.contains("DNS") || errorMsg.contains("dns"))) {
                detailedCause = "DNS 설정에 문제가 있습니다.\nCloudflare DNS 연동이나 도메인 설정을 확인해주세요.";
                solution = "1️⃣ 도메인이 Cloudflare에 등록되어 있는지 확인\n" +
                          "2️⃣ Cloudflare API 토큰이 올바른지 확인\n" +
                          "3️⃣ 도메인의 네임서버가 Cloudflare를 가리키는지 확인";
            } else if (errorMsg != null && (errorMsg.contains("connection") || errorMsg.contains("연결"))) {
                detailedCause = "네트워크 연결에 문제가 있습니다.";
                solution = "1️⃣ 인터넷 연결 상태 확인\n" +
                          "2️⃣ 방화벽 설정 확인\n" +
                          "3️⃣ 잠시 후 다시 시도";
            } else if (errorMsg != null && (errorMsg.contains("permission") || errorMsg.contains("권한"))) {
                detailedCause = "권한 문제가 발생했습니다.";
                solution = "1️⃣ API 토큰의 권한 확인\n" +
                          "2️⃣ 서버 접근 권한 확인\n" +
                          "3️⃣ 관리자 권한으로 다시 시도";
            } else {
                detailedCause = "예상치 못한 오류가 발생했습니다.\n" + (errorMsg != null ? errorMsg : "알 수 없는 오류");
                solution = "1️⃣ 입력한 정보가 모두 올바른지 확인\n" +
                          "2️⃣ 서버가 정상 작동 중인지 확인\n" +
                          "3️⃣ 잠시 후 다시 시도\n" +
                          "4️⃣ 문제가 계속되면 관리자에게 문의";
            }
            
            return "❌ 인증서 생성 실패\n\n" +
                   "🔍 **실패 원인**\n" +
                   detailedCause + "\n\n" +
                   "📋 **에러 상세 정보**\n" +
                   (errorMsg != null ? errorMsg : "에러 메시지 없음") + "\n" +
                   (causeMsg != null && !causeMsg.isEmpty() ? "근본 원인: " + causeMsg + "\n" : "") + "\n" +
                   "💡 **해결 방법**\n" +
                   solution + "\n\n" +
                   "📝 **다음 단계**\n" +
                   "문제를 해결하셨다면 다시 시도해주세요.\n" +
                   "어떤 도메인을 등록하시겠습니까?";
        }
    }

    @Tool("Get a specific certificate by ID. Use this when you need detailed information about a certificate and you have its ID.")
    public String getCertificateById(Long certificateId) {
        log.info("Tool called: getCertificateById with id={}", certificateId);
        
        try {
            Certificate cert = certificateService.findById(certificateId);
            return formatCertificateDetail(cert);
        } catch (Exception e) {
            log.error("Error getting certificate by id", e);
            return String.format("❌ ID %d인 인증서를 찾을 수 없습니다: %s", certificateId, e.getMessage());
        }
    }

    @Tool("Get certificates that are expiring soon (within 30 days). Use this when user asks about certificates that need attention or are about to expire.")
    public String getCertificatesExpiringSoon() {
        log.info("Tool called: getCertificatesExpiringSoon");
        
        try {
            Page<Certificate> certificates = certificateService.findAll(PageRequest.of(0, 100));
            LocalDateTime thirtyDaysLater = LocalDateTime.now().plusDays(30);
            
            List<Certificate> expiringSoon = certificates.getContent().stream()
                    .filter(cert -> cert.getExpiresAt() != null && 
                                   cert.getExpiresAt().isBefore(thirtyDaysLater) &&
                                   cert.getExpiresAt().isAfter(LocalDateTime.now()))
                    .sorted((a, b) -> a.getExpiresAt().compareTo(b.getExpiresAt()))
                    .collect(Collectors.toList());
            
            if (expiringSoon.isEmpty()) {
                return "✅ 30일 이내에 만료될 인증서가 없습니다. 모든 인증서가 안전합니다!";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("⚠️ 30일 이내에 만료될 인증서 %d개:\n\n", expiringSoon.size()));
            
            for (Certificate cert : expiringSoon) {
                long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), cert.getExpiresAt());
                sb.append(String.format(
                    "%s ID: %d | %s | %d일 후 만료 | 만료일: %s\n",
                    daysLeft <= 7 ? "🚨" : "⚠️",
                    cert.getId(),
                    cert.getDomain(),
                    daysLeft,
                    cert.getExpiresAt().toLocalDate()
                ));
            }
            
            sb.append("\n💡 갱신이 필요한 인증서는 자동으로 갱신되거나 수동으로 갱신할 수 있습니다.");
            
            return sb.toString();
        } catch (Exception e) {
            log.error("Error getting expiring certificates", e);
            return "조회 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * 인증서 목록을 포맷팅
     */
    private String formatCertificateList(List<Certificate> certificates) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("총 %d개의 인증서를 찾았습니다:\n\n", certificates.size()));
        
        for (Certificate cert : certificates) {
            sb.append(formatCertificateSummary(cert)).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * 인증서 요약 정보
     */
    private String formatCertificateSummary(Certificate cert) {
        String statusIcon = getStatusIcon(cert.getStatus().name());
        String daysLeft = cert.getExpiresAt() != null 
            ? String.format("%d일 남음", ChronoUnit.DAYS.between(LocalDateTime.now(), cert.getExpiresAt()))
            : "N/A";
        
        return String.format(
            "%s ID: %d | %s | %s | %s",
            statusIcon,
            cert.getId(),
            cert.getDomain(),
            translateStatus(cert.getStatus().name()),
            daysLeft
        );
    }

    /**
     * 인증서 상세 정보
     */
    private String formatCertificateDetail(Certificate cert) {
        String statusIcon = getStatusIcon(cert.getStatus().name());
        String daysLeft = cert.getExpiresAt() != null 
            ? String.format("%d일", ChronoUnit.DAYS.between(LocalDateTime.now(), cert.getExpiresAt()))
            : "N/A";
        
        return String.format(
            "📄 인증서 상세 정보\n\n" +
            "ID: %d\n" +
            "도메인: %s\n" +
            "상태: %s %s\n" +
            "발급일: %s\n" +
            "만료일: %s\n" +
            "남은 기간: %s\n" +
            "관리자: %s\n" +
            "알림 설정: 만료 %d일 전",
            cert.getId(),
            cert.getDomain(),
            statusIcon,
            translateStatus(cert.getStatus().name()),
            cert.getIssuedAt() != null ? cert.getIssuedAt().toLocalDate().toString() : "N/A",
            cert.getExpiresAt() != null ? cert.getExpiresAt().toLocalDate().toString() : "N/A",
            daysLeft,
            cert.getAdmin() != null ? cert.getAdmin() : "미지정",
            cert.getAlertDaysBeforeExpiry() != null ? cert.getAlertDaysBeforeExpiry() : 7
        );
    }

    /**
     * 상태 아이콘
     */
    private String getStatusIcon(String status) {
        switch (status) {
            case "ACTIVE": return "✅";
            case "EXPIRING_SOON": return "⚠️";
            case "EXPIRED": return "❌";
            case "PENDING": return "🔄";
            case "RENEWING": return "🔄";
            case "FAILED": return "❌";
            default: return "❓";
        }
    }

    /**
     * 상태를 한글로 번역
     */
    private String translateStatus(String status) {
        switch (status.toUpperCase()) {
            case "ACTIVE": return "유효";
            case "EXPIRING_SOON": return "곧 만료";
            case "EXPIRED": return "만료됨";
            case "PENDING": return "대기 중";
            case "RENEWING": return "갱신 중";
            case "FAILED": return "실패";
            default: return status;
        }
    }
}


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
            return "❌ 인증서 갱신 실패\n\n" +
                   "🔍 원인: " + errorMsg + "\n\n" +
                   "💡 해결 방법:\n" +
                   "1. 인증서 상태가 갱신 가능한지 확인해주세요\n" +
                   "2. 도메인이 올바른지 확인해주세요\n" +
                   "3. 잠시 후 다시 시도해주세요\n\n" +
                   "📝 다시 시도하시겠습니까? 다른 도메인을 갱신하시겠습니까?";
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
                   "🔍 원인: " + e.getMessage() + "\n\n" +
                   "💡 해결 방법:\n" +
                   "1. 웹 UI의 '서버 관리' 메뉴에서 서버를 먼저 등록해주세요\n" +
                   "2. 서버 등록 후 다시 시도해주세요\n\n" +
                   "📝 다시 시도하시겠습니까? 도메인 이름을 알려주세요.";
        } catch (IllegalArgumentException e) {
            // 도메인 형식이나 기타 인자 오류
            log.error("Error creating certificate - invalid argument", e);
            String errorMsg = e.getMessage().toLowerCase();
            
            if (errorMsg.contains("domain") || errorMsg.contains("도메인")) {
                return "❌ 인증서 생성 실패\n\n" +
                       "🔍 원인: 도메인 형식이 올바르지 않습니다\n\n" +
                       "💡 올바른 도메인 형식:\n" +
                       "- example.com\n" +
                       "- subdomain.example.com\n" +
                       "- example.co.kr\n\n" +
                       "📝 올바른 도메인으로 다시 시도해주세요. 어떤 도메인을 등록하시겠습니까?";
            } else if (errorMsg.contains("이미 존재") || errorMsg.contains("duplicate")) {
                return "⚠️ 인증서 등록 불가\n\n" +
                       "🔍 원인: 이미 등록된 도메인입니다\n\n" +
                       "💡 다음 중 선택해주세요:\n" +
                       "1. 기존 인증서를 갱신하시겠습니까?\n" +
                       "2. 다른 도메인을 등록하시겠습니까?\n\n" +
                       "어떻게 하시겠습니까?";
            } else {
                return "❌ 인증서 생성 실패\n\n" +
                       "🔍 원인: " + e.getMessage() + "\n\n" +
                       "💡 입력 정보를 확인해주세요\n\n" +
                       "📝 다시 시도하시겠습니까? 도메인 이름을 알려주세요.";
            }
        } catch (Exception e) {
            // 기타 예외
            log.error("Error creating certificate", e);
            return "❌ 인증서 생성 중 예상치 못한 오류가 발생했습니다\n\n" +
                   "🔍 오류 내용: " + e.getMessage() + "\n\n" +
                   "💡 해결 방법:\n" +
                   "1. 도메인 형식 확인 (예: example.com)\n" +
                   "2. 서버가 등록되어 있는지 확인\n" +
                   "3. 문제가 계속되면 관리자에게 문의\n\n" +
                   "📝 다른 도메인으로 다시 시도하시겠습니까?";
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

    @Tool("Delete a certificate by ID. Use this ONLY after user explicitly confirms deletion. IMPORTANT: Before calling this, you MUST: 1) Search certificate by domain using searchCertificateByDomain, 2) Show certificate info to user, 3) Warn about consequences, 4) Ask for confirmation, 5) ONLY if user confirms, call this tool with the certificate ID. This action cannot be undone!")
    public String deleteCertificate(Long certificateId) {
        log.info("Tool called: deleteCertificate with id={}", certificateId);
        
        try {
            Certificate cert = certificateService.findById(certificateId);
            String domain = cert.getDomain();
            
            certificateService.delete(certificateId);
            
            return String.format(
                "✅ 인증서가 성공적으로 삭제되었습니다.\n\n" +
                "🗑️ 삭제된 인증서:\n" +
                "- 도메인: %s\n\n" +
                "⚠️ 이 작업은 되돌릴 수 없습니다.",
                domain
            );
        } catch (Exception e) {
            log.error("Error deleting certificate", e);
            return String.format("❌ 인증서 삭제 중 오류가 발생했습니다: %s", e.getMessage());
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


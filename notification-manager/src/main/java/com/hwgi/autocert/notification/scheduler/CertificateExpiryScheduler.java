package com.hwgi.autocert.notification.scheduler;

import com.hwgi.autocert.domain.model.Certificate;
import com.hwgi.autocert.domain.model.CertificateStatus;
import com.hwgi.autocert.domain.repository.CertificateRepository;
import com.hwgi.autocert.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 인증서 만료 체크 스케줄러
 * 
 * 매일 오전 9시에 실행되어 만료 임박 인증서를 확인하고 이메일 알림을 발송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "app.email",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class CertificateExpiryScheduler {
    
    private final CertificateRepository certificateRepository;
    private final EmailService emailService;
    
    /**
     * 만료 임박 인증서 체크 및 알림 발송
     * 
     * 크론 표현식: "0 0 9 * * *"
     * - 매일 오전 9시 정각에 실행
     * - Asia/Seoul 시간대 기준
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void checkExpiringCertificates() {
        log.info("🔍 Starting daily expiring certificates check...");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sevenDaysLater = now.plusDays(7);
            
            // 7일 이내 만료되는 ACTIVE 상태의 인증서 조회
            List<Certificate> expiringCertificates = certificateRepository.findAll()
                .stream()
                .filter(cert -> cert.getExpiresAt() != null)
                .filter(cert -> cert.getExpiresAt().isAfter(now))
                .filter(cert -> cert.getExpiresAt().isBefore(sevenDaysLater))
                .filter(cert -> cert.getStatus() == CertificateStatus.ACTIVE)
                .sorted((a, b) -> a.getExpiresAt().compareTo(b.getExpiresAt()))
                .collect(Collectors.toList());
            
            if (!expiringCertificates.isEmpty()) {
                log.info("📧 Found {} certificates expiring within 7 days", expiringCertificates.size());
                
                // 만료 임박 인증서 로그 출력
                expiringCertificates.forEach(cert -> {
                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, cert.getExpiresAt());
                    log.info("  - {} expires in {} days ({})", 
                            cert.getDomain(), daysLeft, cert.getExpiresAt());
                });
                
                // 이메일 발송
                emailService.sendExpiringCertificatesAlert(expiringCertificates);
            } else {
                log.info("✅ No certificates expiring within 7 days");
            }
        } catch (Exception e) {
            log.error("❌ Failed to check expiring certificates", e);
        }
        
        log.info("✅ Daily expiring certificates check completed");
    }
    
    /**
     * 수동 테스트용 메서드 (스케줄 없이 즉시 실행 가능)
     * 
     * API 엔드포인트나 테스트 코드에서 호출 가능
     */
    public void checkExpiringCertificatesManually() {
        log.info("🔧 Manual check triggered");
        checkExpiringCertificates();
    }
}

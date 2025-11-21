package com.hwgi.autocert.notification.service;

import com.hwgi.autocert.domain.model.Certificate;
import com.hwgi.autocert.notification.config.EmailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 이메일 알림 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;
    private final SpringTemplateEngine templateEngine;
    
    @Override
    public void sendCertificateCreated(Certificate certificate) {
        if (!emailProperties.isEnabled()) {
            log.debug("Email notification is disabled");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // 수신자 설정 (여러 명에게 발송)
            String[] recipients = emailProperties.getRecipients().toArray(new String[0]);
            helper.setTo(recipients);
            helper.setFrom(emailProperties.getFrom(), emailProperties.getFromName());
            helper.setSubject("[AutoCert] 새 인증서 생성 완료: " + certificate.getDomain());
            
            // 템플릿 컨텍스트 설정
            Context context = new Context();
            context.setVariable("domain", certificate.getDomain());
            context.setVariable("issuedAt", certificate.getIssuedAt());
            context.setVariable("expiresAt", certificate.getExpiresAt());
            context.setVariable("status", certificate.getStatus());
            context.setVariable("admin", certificate.getAdmin());
            
            // HTML 생성 및 설정
            String html = templateEngine.process("email/certificate-created", context);
            helper.setText(html, true);
            
            mailSender.send(message);
            log.info("✅ Certificate creation email sent to {} recipients for domain: {}", 
                    recipients.length, certificate.getDomain());
        } catch (Exception e) {
            log.error("❌ Failed to send certificate creation email for domain: {}", 
                    certificate.getDomain(), e);
        }
    }
    
    @Override
    public void sendCertificateRenewed(Certificate certificate) {
        if (!emailProperties.isEnabled()) {
            log.debug("Email notification is disabled");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // 수신자 설정
            String[] recipients = emailProperties.getRecipients().toArray(new String[0]);
            helper.setTo(recipients);
            helper.setFrom(emailProperties.getFrom(), emailProperties.getFromName());
            helper.setSubject("[AutoCert] 인증서 갱신 완료: " + certificate.getDomain());
            
            // 템플릿 컨텍스트 설정
            Context context = new Context();
            context.setVariable("domain", certificate.getDomain());
            context.setVariable("issuedAt", certificate.getIssuedAt());
            context.setVariable("expiresAt", certificate.getExpiresAt());
            context.setVariable("status", certificate.getStatus());
            
            // HTML 생성 및 설정
            String html = templateEngine.process("email/certificate-renewed", context);
            helper.setText(html, true);
            
            mailSender.send(message);
            log.info("✅ Certificate renewal email sent to {} recipients for domain: {}", 
                    recipients.length, certificate.getDomain());
        } catch (Exception e) {
            log.error("❌ Failed to send certificate renewal email for domain: {}", 
                    certificate.getDomain(), e);
        }
    }
    
    @Override
    public void sendCertificateDeleted(String domain) {
        if (!emailProperties.isEnabled()) {
            log.debug("Email notification is disabled");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // 수신자 설정
            String[] recipients = emailProperties.getRecipients().toArray(new String[0]);
            helper.setTo(recipients);
            helper.setFrom(emailProperties.getFrom(), emailProperties.getFromName());
            helper.setSubject("[AutoCert] 인증서 삭제 완료: " + domain);
            
            // 템플릿 컨텍스트 설정
            Context context = new Context();
            context.setVariable("domain", domain);
            context.setVariable("deletedAt", LocalDateTime.now());
            
            // HTML 생성 및 설정
            String html = templateEngine.process("email/certificate-deleted", context);
            helper.setText(html, true);
            
            mailSender.send(message);
            log.info("✅ Certificate deletion email sent to {} recipients for domain: {}", 
                    recipients.length, domain);
        } catch (Exception e) {
            log.error("❌ Failed to send certificate deletion email for domain: {}", domain, e);
        }
    }
    
    @Override
    public void sendExpiringCertificatesAlert(List<Certificate> certificates) {
        if (!emailProperties.isEnabled()) {
            log.debug("Email notification is disabled");
            return;
        }
        
        if (certificates.isEmpty()) {
            log.debug("No expiring certificates to notify");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // 수신자 설정
            String[] recipients = emailProperties.getRecipients().toArray(new String[0]);
            helper.setTo(recipients);
            helper.setFrom(emailProperties.getFrom(), emailProperties.getFromName());
            helper.setSubject("[AutoCert] 인증서 만료 임박 알림 - " + certificates.size() + "건");
            
            // 템플릿 컨텍스트 설정
            Context context = new Context();
            context.setVariable("certificates", certificates);
            context.setVariable("count", certificates.size());
            context.setVariable("checkDate", LocalDateTime.now());
            
            // 각 인증서의 남은 일수 계산하여 추가
            LocalDateTime now = LocalDateTime.now();
            certificates.forEach(cert -> {
                if (cert.getExpiresAt() != null) {
                    long daysLeft = ChronoUnit.DAYS.between(now, cert.getExpiresAt());
                    cert.setAlertDaysBeforeExpiry((int) daysLeft); // 임시로 저장
                }
            });
            
            // HTML 생성 및 설정
            String html = templateEngine.process("email/expiring-certificates", context);
            helper.setText(html, true);
            
            mailSender.send(message);
            log.info("✅ Expiring certificates alert sent to {} recipients: {} certificates", 
                    recipients.length, certificates.size());
        } catch (Exception e) {
            log.error("❌ Failed to send expiring certificates alert", e);
        }
    }
}

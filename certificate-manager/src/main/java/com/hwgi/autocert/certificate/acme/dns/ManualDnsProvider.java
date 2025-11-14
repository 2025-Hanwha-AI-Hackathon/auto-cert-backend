package com.hwgi.autocert.certificate.acme.dns;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 수동 DNS 프로바이더
 * 
 * 사용자가 수동으로 DNS TXT 레코드를 추가/삭제하는 방식
 * 개발/테스트 환경이나 DNS API가 없는 경우 사용
 */
@Slf4j
@Component
public class ManualDnsProvider implements DnsProvider {

    private static final String PROVIDER_NAME = "manual";

    @Override
    public void addTxtRecord(String domain, String recordName, String recordValue) {
        log.info("=".repeat(80));
        log.info("MANUAL DNS CHALLENGE - TXT 레코드 추가 필요");
        log.info("=".repeat(80));
        log.info("도메인: {}", domain);
        log.info("레코드명: {}.{}", recordName, domain);
        log.info("레코드타입: TXT");
        log.info("레코드값: {}", recordValue);
        log.info("=".repeat(80));
        log.info("위 정보로 DNS TXT 레코드를 추가한 후 Enter를 눌러주세요...");
        log.info("=".repeat(80));
        
        // 사용자 입력 대기 (프로덕션에서는 다른 방식 사용)
        // TODO: 웹 UI나 API를 통한 확인 방식으로 개선
        waitForUserConfirmation();
    }

    @Override
    public void removeTxtRecord(String domain, String recordName, String recordValue) {
        log.info("DNS TXT 레코드를 삭제하세요: {}.{} = {}", recordName, domain, recordValue);
        log.info("(자동으로 계속 진행됩니다)");
    }

    @Override
    public boolean waitForPropagation(String domain, String recordName, String recordValue, int timeoutSeconds) {
        log.info("DNS 전파 대기 중... (최대 {}초)", timeoutSeconds);
        
        // 수동 모드에서는 사용자가 확인했다고 가정하고 짧은 대기만 수행
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        
        log.info("DNS 전파 완료로 간주");
        return true;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * 사용자 확인 대기
     * 
     * TODO: 프로덕션 환경에서는 웹 UI나 API를 통한 확인 방식으로 개선 필요
     */
    private void waitForUserConfirmation() {
        // 환경변수로 자동 진행 가능
        String autoConfirm = System.getenv("ACME_AUTO_CONFIRM");
        if ("true".equalsIgnoreCase(autoConfirm)) {
            log.info("AUTO_CONFIRM 모드: 자동으로 계속 진행합니다");
            return;
        }

        // 대화형 모드에서는 사용자 입력 대기
        try (Scanner scanner = new Scanner(System.in)) {
            scanner.nextLine();
        } catch (Exception e) {
            log.warn("사용자 입력 대기 중 오류 발생, 자동으로 계속 진행합니다", e);
        }
    }
}

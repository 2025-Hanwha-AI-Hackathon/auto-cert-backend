package com.hwgi.autocert.certificate.acme.challenge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

/**
 * HTTP-01 챌린지 핸들러
 * 
 * 웹서버의 /.well-known/acme-challenge/ 경로에 토큰 파일을 배치하여 도메인 소유권 증명
 * 
 * 장점:
 * - 간단한 구현
 * - 포트 80만 열려있으면 가능
 * 
 * 단점:
 * - 와일드카드 인증서 불가
 * - 방화벽 뒤 서버는 어려움
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Http01ChallengeHandler implements ChallengeHandler {

    private static final String WELL_KNOWN_PATH = "/.well-known/acme-challenge/";

    // 웹서버 루트 디렉토리 (설정에서 주입 가능)
    private static final String DEFAULT_WEBROOT = "/var/www/html";

    /**
     * HTTP-01 챌린지 준비
     * 
     * 웹서버의 .well-known/acme-challenge/ 경로에 토큰 파일 생성
     */
    @Override
    public void prepare(String domain, Challenge challenge) throws Exception {
        if (!(challenge instanceof Http01Challenge)) {
            throw new IllegalArgumentException("Challenge must be Http01Challenge");
        }

        Http01Challenge http01 = (Http01Challenge) challenge;
        
        String token = http01.getToken();
        String content = http01.getAuthorization();
        
        log.info("Preparing HTTP-01 challenge for domain: {}", domain);
        log.debug("Token: {}", token);
        log.debug("Authorization: {}", content);

        // .well-known/acme-challenge 디렉토리 생성
        Path challengeDir = Paths.get(DEFAULT_WEBROOT, ".well-known", "acme-challenge");
        if (!Files.exists(challengeDir)) {
            Files.createDirectories(challengeDir);
            log.info("Created challenge directory: {}", challengeDir);
        }

        // 토큰 파일 생성
        Path tokenFile = challengeDir.resolve(token);
        Files.write(tokenFile, content.getBytes(), StandardOpenOption.CREATE);
        
        log.info("HTTP-01 challenge file created: {}", tokenFile);
        log.info("Challenge URL: http://{}{}{}",domain, WELL_KNOWN_PATH, token);
        
        // 파일 접근 권한 설정 (읽기 가능하게)
        tokenFile.toFile().setReadable(true, false);
    }

    /**
     * HTTP-01 챌린지 검증
     * 
     * ACME 서버가 HTTP로 토큰 파일에 접근하여 검증
     */
    @Override
    public void validate(Challenge challenge) throws Exception {
        if (!(challenge instanceof Http01Challenge)) {
            throw new IllegalArgumentException("Challenge must be Http01Challenge");
        }

        log.info("Triggering HTTP-01 challenge validation");
        
        // 챌린지 트리거
        challenge.trigger();
        
        // 챌린지 상태 폴링 (최대 3분)
        int attempts = 0;
        int maxAttempts = 60; // 3분 (3초 간격)
        
        while (challenge.getStatus() != org.shredzone.acme4j.Status.VALID && attempts < maxAttempts) {
            if (challenge.getStatus() == org.shredzone.acme4j.Status.INVALID) {
                throw new AcmeException("HTTP-01 challenge validation failed: " + 
                    challenge.getError().map(error -> error.toString()).orElse("Unknown error"));
            }
            
            // 3초 대기
            TimeUnit.SECONDS.sleep(3);
            
            // 상태 업데이트
            challenge.update();
            attempts++;
            
            log.debug("Challenge status: {} (attempt {}/{})", 
                challenge.getStatus(), attempts, maxAttempts);
        }
        
        if (challenge.getStatus() != org.shredzone.acme4j.Status.VALID) {
            throw new AcmeException("HTTP-01 challenge validation timeout");
        }
        
        log.info("HTTP-01 challenge validated successfully");
    }

    /**
     * HTTP-01 챌린지 정리
     * 
     * 생성된 토큰 파일 삭제
     */
    @Override
    public void cleanup(String domain, Challenge challenge) {
        if (!(challenge instanceof Http01Challenge)) {
            log.warn("Challenge is not Http01Challenge, skipping cleanup");
            return;
        }

        Http01Challenge http01 = (Http01Challenge) challenge;
        String token = http01.getToken();
        
        Path tokenFile = Paths.get(DEFAULT_WEBROOT, ".well-known", "acme-challenge", token);
        
        try {
            if (Files.exists(tokenFile)) {
                Files.delete(tokenFile);
                log.info("HTTP-01 challenge file deleted: {}", tokenFile);
            }
        } catch (IOException e) {
            log.warn("Failed to delete challenge file: {}", tokenFile, e);
        }
    }

    @Override
    public ChallengeType getChallengeType() {
        return ChallengeType.HTTP_01;
    }
}

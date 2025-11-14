package com.hwgi.autocert.certificate.acme.challenge;

import org.shredzone.acme4j.challenge.Challenge;

/**
 * ACME 챌린지 핸들러 인터페이스
 * 
 * ACME 프로토콜의 도메인 소유권 검증을 위한 챌린지 처리
 */
public interface ChallengeHandler {

    /**
     * 챌린지 준비
     * 
     * 도메인 소유권 증명을 위한 리소스 배치
     * - HTTP-01: 웹서버에 토큰 파일 배치
     * - DNS-01: DNS TXT 레코드 추가
     * 
     * @param domain 도메인명
     * @param challenge ACME 챌린지 객체
     * @throws Exception 챌린지 준비 실패
     */
    void prepare(String domain, Challenge challenge) throws Exception;

    /**
     * 챌린지 검증
     * 
     * ACME 서버가 챌린지를 검증할 수 있도록 트리거
     * 
     * @param challenge ACME 챌린지 객체
     * @throws Exception 챌린지 검증 실패
     */
    void validate(Challenge challenge) throws Exception;

    /**
     * 챌린지 정리
     * 
     * 검증 완료 후 생성된 리소스 정리
     * - HTTP-01: 토큰 파일 삭제
     * - DNS-01: TXT 레코드 삭제
     * 
     * @param domain 도메인명
     * @param challenge ACME 챌린지 객체
     */
    void cleanup(String domain, Challenge challenge);

    /**
     * 챌린지 타입 반환
     *
     * @return 챌린지 타입 (HTTP_01, DNS_01, TLS_ALPN_01)
     */
    ChallengeType getChallengeType();
}

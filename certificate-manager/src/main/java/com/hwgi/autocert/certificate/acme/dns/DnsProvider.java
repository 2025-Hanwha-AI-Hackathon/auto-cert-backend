package com.hwgi.autocert.certificate.acme.dns;

/**
 * DNS 프로바이더 인터페이스
 * 
 * DNS-01 챌린지를 위한 TXT 레코드 관리
 */
public interface DnsProvider {

    /**
     * TXT 레코드 추가
     * 
     * @param domain 도메인명
     * @param recordName 레코드 이름 (예: _acme-challenge)
     * @param recordValue 레코드 값
     * @throws Exception TXT 레코드 추가 실패
     */
    void addTxtRecord(String domain, String recordName, String recordValue) throws Exception;

    /**
     * TXT 레코드 삭제
     * 
     * @param domain 도메인명
     * @param recordName 레코드 이름
     * @param recordValue 레코드 값
     * @throws Exception TXT 레코드 삭제 실패
     */
    void removeTxtRecord(String domain, String recordName, String recordValue) throws Exception;

    /**
     * DNS 전파 대기
     * 
     * DNS 변경사항이 전파될 때까지 대기
     * 
     * @param domain 도메인명
     * @param recordName 레코드 이름
     * @param recordValue 예상 레코드 값
     * @param timeoutSeconds 타임아웃 (초)
     * @return 전파 성공 여부
     */
    boolean waitForPropagation(String domain, String recordName, String recordValue, int timeoutSeconds);

    /**
     * 프로바이더 이름 반환
     * 
     * @return 프로바이더 이름 (route53, cloudflare, manual 등)
     */
    String getProviderName();
}

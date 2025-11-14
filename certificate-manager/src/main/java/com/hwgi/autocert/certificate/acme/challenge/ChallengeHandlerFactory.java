package com.hwgi.autocert.certificate.acme.challenge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 챌린지 핸들러 팩토리
 *
 * 챌린지 타입에 따라 적절한 핸들러 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeHandlerFactory {

    private final List<ChallengeHandler> challengeHandlers;
    private final Map<ChallengeType, ChallengeHandler> handlerMap = new EnumMap<>(ChallengeType.class);

    /**
     * 초기화 - 모든 핸들러를 맵에 등록
     */
    public void init() {
        for (ChallengeHandler handler : challengeHandlers) {
            handlerMap.put(handler.getChallengeType(), handler);
            log.info("Registered challenge handler: {}", handler.getChallengeType());
        }
    }

    /**
     * 챌린지 타입에 맞는 핸들러 반환
     *
     * @param challengeType 챌린지 타입 (HTTP_01, DNS_01, TLS_ALPN_01)
     * @return 챌린지 핸들러
     * @throws IllegalArgumentException 지원하지 않는 챌린지 타입
     */
    public ChallengeHandler getHandler(ChallengeType challengeType) {
        // 초기화 확인
        if (handlerMap.isEmpty()) {
            init();
        }

        ChallengeHandler handler = handlerMap.get(challengeType);

        if (handler == null) {
            throw new IllegalArgumentException(
                "Unsupported challenge type: " + challengeType +
                ". Supported types: " + handlerMap.keySet()
            );
        }

        return handler;
    }

    /**
     * 문자열로부터 챌린지 타입 핸들러 반환
     *
     * @param challengeType 챌린지 타입 문자열 (http-01, dns-01, tls-alpn-01)
     * @return 챌린지 핸들러
     * @throws IllegalArgumentException 지원하지 않는 챌린지 타입
     */
    public ChallengeHandler getHandler(String challengeType) {
        ChallengeType type = ChallengeType.fromValue(challengeType);
        return getHandler(type);
    }

    /**
     * HTTP-01 핸들러 반환
     */
    public ChallengeHandler getHttp01Handler() {
        return getHandler(ChallengeType.HTTP_01);
    }

    /**
     * DNS-01 핸들러 반환 (기본 핸들러)
     */
    public ChallengeHandler getDns01Handler() {
        return getHandler(ChallengeType.DNS_01);
    }

    /**
     * 기본 핸들러 반환 (DNS-01)
     *
     * @return DNS-01 챌린지 핸들러
     */
    public ChallengeHandler getDefaultHandler() {
        return getHandler(ChallengeType.getDefault());
    }

    /**
     * 지원하는 챌린지 타입 목록 반환
     */
    public ChallengeType[] getSupportedTypes() {
        if (handlerMap.isEmpty()) {
            init();
        }
        return handlerMap.keySet().toArray(new ChallengeType[0]);
    }
}

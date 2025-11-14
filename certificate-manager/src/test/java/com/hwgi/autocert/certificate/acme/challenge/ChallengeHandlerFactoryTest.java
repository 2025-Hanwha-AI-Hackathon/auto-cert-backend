package com.hwgi.autocert.certificate.acme.challenge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * ChallengeHandlerFactory 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengeHandlerFactory 테스트")
class ChallengeHandlerFactoryTest {

    @Mock
    private ChallengeHandler http01Handler;

    @Mock
    private ChallengeHandler dns01Handler;

    private ChallengeHandlerFactory factory;

    @BeforeEach
    void setUp() {
        when(http01Handler.getChallengeType()).thenReturn(ChallengeType.HTTP_01);
        when(dns01Handler.getChallengeType()).thenReturn(ChallengeType.DNS_01);

        factory = new ChallengeHandlerFactory(Arrays.asList(http01Handler, dns01Handler));
        factory.init();
    }

    @Test
    @DisplayName("HTTP-01 핸들러 조회 성공")
    void getHttp01Handler_Success() {
        // When
        ChallengeHandler handler = factory.getHttp01Handler();

        // Then
        assertThat(handler).isNotNull();
        assertThat(handler.getChallengeType()).isEqualTo(ChallengeType.HTTP_01);
    }

    @Test
    @DisplayName("DNS-01 핸들러 조회 성공")
    void getDns01Handler_Success() {
        // When
        ChallengeHandler handler = factory.getDns01Handler();

        // Then
        assertThat(handler).isNotNull();
        assertThat(handler.getChallengeType()).isEqualTo(ChallengeType.DNS_01);
    }

    @Test
    @DisplayName("챌린지 타입으로 핸들러 조회 성공 (ENUM)")
    void getHandler_Success() {
        // When
        ChallengeHandler handler = factory.getHandler(ChallengeType.HTTP_01);

        // Then
        assertThat(handler).isNotNull();
        assertThat(handler.getChallengeType()).isEqualTo(ChallengeType.HTTP_01);
    }

    @Test
    @DisplayName("챌린지 타입으로 핸들러 조회 성공 (String)")
    void getHandler_WithString_Success() {
        // When
        ChallengeHandler handler = factory.getHandler("http-01");

        // Then
        assertThat(handler).isNotNull();
        assertThat(handler.getChallengeType()).isEqualTo(ChallengeType.HTTP_01);
    }

    @Test
    @DisplayName("지원하지 않는 챌린지 타입 조회 시 예외 발생")
    void getHandler_UnsupportedType_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> factory.getHandler("tls-alpn-01"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported challenge type");
    }

    @Test
    @DisplayName("지원하는 챌린지 타입 목록 반환")
    void getSupportedTypes_Success() {
        // When
        ChallengeType[] types = factory.getSupportedTypes();

        // Then
        assertThat(types).hasSize(2);
        assertThat(types).containsExactlyInAnyOrder(ChallengeType.HTTP_01, ChallengeType.DNS_01);
    }

    @Test
    @DisplayName("기본 핸들러 조회 성공 (DNS-01)")
    void getDefaultHandler_Success() {
        // When
        ChallengeHandler handler = factory.getDefaultHandler();

        // Then
        assertThat(handler).isNotNull();
        assertThat(handler.getChallengeType()).isEqualTo(ChallengeType.DNS_01);
    }
}

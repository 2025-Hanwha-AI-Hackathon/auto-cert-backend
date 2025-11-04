package com.hwgi.autocert.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DateTimeUtils 테스트")
class DateTimeUtilsTest {

    @Test
    @DisplayName("두 날짜 사이의 일수 계산")
    void testDaysBetween() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 31, 0, 0);

        long days = DateTimeUtils.daysBetween(start, end);

        assertEquals(30, days);
    }

    @Test
    @DisplayName("날짜 더하기")
    void testPlusDays() {
        LocalDateTime base = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2025, 1, 11, 0, 0);

        LocalDateTime result = DateTimeUtils.plusDays(base, 10);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("날짜 빼기")
    void testMinusDays() {
        LocalDateTime base = LocalDateTime.of(2025, 1, 11, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2025, 1, 1, 0, 0);

        LocalDateTime result = DateTimeUtils.minusDays(base, 10);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("만료 여부 확인 - 만료됨")
    void testIsExpired_Expired() {
        LocalDateTime expiryDate = LocalDateTime.now().minusDays(1);

        assertTrue(DateTimeUtils.isExpired(expiryDate));
    }

    @Test
    @DisplayName("만료 여부 확인 - 만료 안됨")
    void testIsExpired_NotExpired() {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(30);

        assertFalse(DateTimeUtils.isExpired(expiryDate));
    }

    @Test
    @DisplayName("곧 만료될 예정인지 확인 - 30일 이내")
    void testIsExpiringSoon_True() {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(20);

        assertTrue(DateTimeUtils.isExpiringSoon(expiryDate, 30));
    }

    @Test
    @DisplayName("곧 만료될 예정인지 확인 - 30일 초과")
    void testIsExpiringSoon_False() {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(40);

        assertFalse(DateTimeUtils.isExpiringSoon(expiryDate, 30));
    }

    @Test
    @DisplayName("날짜 포맷")
    void testFormat() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 15, 10, 30, 45);

        String formatted = DateTimeUtils.format(dateTime);

        assertEquals("2025-01-15T10:30:45", formatted);
    }

    @Test
    @DisplayName("날짜 파싱")
    void testParse() {
        String dateTimeString = "2025-01-15T10:30:45";

        LocalDateTime parsed = DateTimeUtils.parse(dateTimeString);

        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30, 45), parsed);
    }
}
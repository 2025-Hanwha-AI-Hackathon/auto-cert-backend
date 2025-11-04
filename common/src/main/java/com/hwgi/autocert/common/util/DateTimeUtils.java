package com.hwgi.autocert.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 날짜/시간 처리 유틸리티
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
    public static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * 현재 시간 반환 (시스템 기본 시간대)
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE_ID);
    }

    /**
     * 현재 UTC 시간 반환
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC);
    }

    /**
     * 현재 날짜 반환
     */
    public static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE_ID);
    }

    /**
     * 두 날짜 사이의 일수 계산
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 두 날짜 사이의 일수 계산
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 지정한 일수만큼 이후의 날짜 반환
     */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        return dateTime.plusDays(days);
    }

    /**
     * 지정한 일수만큼 이전의 날짜 반환
     */
    public static LocalDateTime minusDays(LocalDateTime dateTime, long days) {
        return dateTime.minusDays(days);
    }

    /**
     * LocalDateTime을 Instant로 변환
     */
    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.atZone(DEFAULT_ZONE_ID).toInstant();
    }

    /**
     * Instant를 LocalDateTime으로 변환
     */
    public static LocalDateTime fromInstant(Instant instant) {
        return LocalDateTime.ofInstant(instant, DEFAULT_ZONE_ID);
    }

    /**
     * 날짜를 문자열로 포맷
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime.format(ISO_DATE_TIME);
    }

    /**
     * 날짜를 지정한 포맷으로 변환
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime.format(formatter);
    }

    /**
     * 문자열을 LocalDateTime으로 파싱
     */
    public static LocalDateTime parse(String dateTimeString) {
        return LocalDateTime.parse(dateTimeString, ISO_DATE_TIME);
    }

    /**
     * 문자열을 지정한 포맷으로 파싱
     */
    public static LocalDateTime parse(String dateTimeString, DateTimeFormatter formatter) {
        return LocalDateTime.parse(dateTimeString, formatter);
    }

    /**
     * 날짜가 특정 범위 내에 있는지 확인
     */
    public static boolean isBetween(LocalDateTime target, LocalDateTime start, LocalDateTime end) {
        return !target.isBefore(start) && !target.isAfter(end);
    }

    /**
     * 만료까지 남은 일수 계산
     */
    public static long daysUntilExpiry(LocalDateTime expiryDate) {
        return daysBetween(now(), expiryDate);
    }

    /**
     * 만료 여부 확인
     */
    public static boolean isExpired(LocalDateTime expiryDate) {
        return expiryDate.isBefore(now());
    }

    /**
     * 곧 만료될 예정인지 확인 (지정한 일수 이내)
     */
    public static boolean isExpiringSoon(LocalDateTime expiryDate, int daysThreshold) {
        long daysUntil = daysUntilExpiry(expiryDate);
        return daysUntil >= 0 && daysUntil <= daysThreshold;
    }
}

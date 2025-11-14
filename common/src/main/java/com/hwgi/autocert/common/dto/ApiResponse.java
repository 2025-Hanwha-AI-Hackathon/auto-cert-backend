package com.hwgi.autocert.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 표준 API 응답 래퍼 클래스
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 성공 여부
     */
    private boolean success;

    /**
     * 응답 데이터
     */
    private T data;

    /**
     * 응답 메시지
     */
    private String message;

    /**
     * 에러 코드 (실패 시)
     */
    private String errorCode;

    /**
     * 타임스탬프
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 성공 응답 생성 (데이터 포함)
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message("Success")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 성공 응답 생성 (메시지 포함)
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성 (데이터 포함)
     */
    public static <T> ApiResponse<T> error(String errorCode, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
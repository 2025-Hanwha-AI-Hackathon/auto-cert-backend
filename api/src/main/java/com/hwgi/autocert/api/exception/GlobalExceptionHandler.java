package com.hwgi.autocert.api.exception;

import com.hwgi.autocert.common.dto.ApiResponse;
import com.hwgi.autocert.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * 애플리케이션 전체에서 발생하는 예외를 일관된 형식으로 처리합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ResourceNotFoundException 처리
     * 리소스를 찾을 수 없을 때 404 Not Found 응답
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {} - Request: {}", ex.getMessage(), request.getDescription(false));

        ApiResponse<Void> response = ApiResponse.error(
            "RESOURCE_NOT_FOUND",
            ex.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response);
    }

    /**
     * IllegalArgumentException 처리
     * 잘못된 인자가 전달되었을 때 400 Bad Request 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {} - Request: {}", ex.getMessage(), request.getDescription(false));

        ApiResponse<Void> response = ApiResponse.error(
            "INVALID_ARGUMENT",
            ex.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }

    /**
     * IllegalStateException 처리
     * 잘못된 상태에서 작업을 시도했을 때 409 Conflict 응답
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state: {} - Request: {}", ex.getMessage(), request.getDescription(false));

        ApiResponse<Void> response = ApiResponse.error(
            "INVALID_STATE",
            ex.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response);
    }

    /**
     * Validation 실패 처리 (@Valid 검증 실패)
     * 400 Bad Request 응답과 함께 필드별 에러 정보 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.error(
            "VALIDATION_FAILED",
            "입력 값 검증에 실패했습니다",
            errors
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }

    /**
     * 일반 RuntimeException 처리
     * 500 Internal Server Error 응답
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        log.error("Runtime exception occurred: {} - Request: {}",
            ex.getMessage(), request.getDescription(false), ex);

        ApiResponse<Void> response = ApiResponse.error(
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다: " + ex.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }

    /**
     * 모든 예외의 최종 처리
     * 500 Internal Server Error 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {
        log.error("Unexpected exception occurred: {} - Request: {}",
            ex.getMessage(), request.getDescription(false), ex);

        ApiResponse<Void> response = ApiResponse.error(
            "INTERNAL_SERVER_ERROR",
            "예기치 않은 오류가 발생했습니다"
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}

package com.hwgi.autocert.api.controller;

import com.hwgi.autocert.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 헬스체크 API
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "헬스체크 API")
public class HealthController {

    @Operation(summary = "헬스체크", description = "서버 상태 확인")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "auto-cert");

        return ApiResponse.success(health, "서비스가 정상 작동 중입니다");
    }
}
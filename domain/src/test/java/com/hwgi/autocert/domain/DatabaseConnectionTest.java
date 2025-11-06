package com.hwgi.autocert.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.hwgi.autocert.common.config.CommonConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestApplication.class)
class DatabaseConnectionTest {

    @Test
    @DisplayName("데이터베이스 연결 및 Flyway 마이그레이션 테스트")
    void contextLoads() {
        // Spring Boot 컨텍스트가 성공적으로 로드되면,
        // 데이터베이스 연결 및 Flyway 마이그레이션이 성공적으로 수행된 것으로 간주할 수 있습니다.
        assertTrue(true, "애플리케이션 컨텍스트 로딩에 성공해야 합니다.");
    }
}

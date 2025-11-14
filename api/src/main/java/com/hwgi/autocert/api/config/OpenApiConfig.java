package com.hwgi.autocert.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger springdoc-ui 구성 파일
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("API Document")
                .version("v0.0.1")
                .description("API 명세서입니다.");

        return new OpenAPI()
                .components(new Components())
                .addServersItem(new Server().url("/")) // 추가
                .info(info);
    }
}

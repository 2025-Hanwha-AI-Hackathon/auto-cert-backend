package com.hwgi.autocert.api.controller;

import com.hwgi.autocert.server.dto.ServerCreateRequest;
import com.hwgi.autocert.server.dto.ServerUpdateRequest;
import com.hwgi.autocert.server.dto.ServerResponse;
import com.hwgi.autocert.server.service.ServerService;
import com.hwgi.autocert.api.dto.response.PageResponse;
import com.hwgi.autocert.common.dto.ApiResponse;
import com.hwgi.autocert.domain.model.Server;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 서버 관리 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
@Tag(name = "Server", description = "서버 관리 API")
public class ServerController {

    private final ServerService serverService;

    @Operation(summary = "서버 목록 조회", description = "페이지네이션을 지원하는 서버 목록 조회")
    @GetMapping
    public ApiResponse<PageResponse<ServerResponse>> getServers(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Get servers list - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Server> page = serverService.findAll(pageable);
        PageResponse<ServerResponse> response = PageResponse.from(page.map(ServerResponse::from));
        return ApiResponse.success(response, "서버 목록 조회 성공");
    }

    @Operation(summary = "서버 상세 조회", description = "ID로 특정 서버 조회")
    @GetMapping("/{id}")
    public ApiResponse<ServerResponse> getServer(@PathVariable Long id) {
        log.info("Get server by id: {}", id);

        Server server = serverService.findById(id);
        ServerResponse response = ServerResponse.from(server);
        return ApiResponse.success(response, "서버 조회 성공");
    }

    @Operation(summary = "웹서버 타입별 조회", description = "웹서버 타입(nginx, apache, tomcat, iis)으로 서버 목록 조회")
    @GetMapping("/type/{webServerType}")
    public ApiResponse<PageResponse<ServerResponse>> getServersByType(
            @PathVariable String webServerType,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Get servers by type: {}", webServerType);

        Page<Server> page = serverService.findByWebServerType(webServerType, pageable);
        PageResponse<ServerResponse> response = PageResponse.from(page.map(ServerResponse::from));
        return ApiResponse.success(response, "서버 목록 조회 성공");
    }

    @Operation(summary = "서버 검색", description = "서버 이름 또는 IP 주소로 검색")
    @GetMapping("/search")
    public ApiResponse<PageResponse<ServerResponse>> searchServers(
            @RequestParam String query,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Search servers by query: {}", query);

        Page<Server> page = serverService.searchByNameOrIp(query, pageable);
        PageResponse<ServerResponse> response = PageResponse.from(page.map(ServerResponse::from));
        return ApiResponse.success(response, "서버 검색 성공");
    }

    @Operation(summary = "서버 생성", description = "새로운 서버 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ServerResponse> createServer(
            @Valid @RequestBody ServerCreateRequest request) {
        log.info("Create server: name={}, ip={}", request.getName(), request.getIpAddress());

        Server server = serverService.create(
                request.getName(),
                request.getIpAddress(),
                request.getPort(),
                request.getWebServerType(),
                request.getDescription(),
                request.getUsername(),
                request.getPassword(),
                request.getDeployPath()
        );

        ServerResponse response = ServerResponse.from(server);
        return ApiResponse.success(response, "서버 생성 성공");
    }

    @Operation(summary = "서버 수정", description = "기존 서버 정보 수정")
    @PutMapping("/{id}")
    public ApiResponse<ServerResponse> updateServer(
            @PathVariable Long id,
            @Valid @RequestBody ServerUpdateRequest request) {
        log.info("Update server: id={}", id);

        Server server = serverService.update(
                id,
                request.getName(),
                request.getIpAddress(),
                request.getPort(),
                request.getWebServerType(),
                request.getDescription(),
                request.getUsername(),
                request.getPassword(),
                request.getDeployPath()
        );

        ServerResponse response = ServerResponse.from(server);
        return ApiResponse.success(response, "서버 수정 성공");
    }

    @Operation(summary = "서버 삭제", description = "서버 삭제 (주의: 복구 불가)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteServer(@PathVariable Long id) {
        log.info("Delete server: id={}", id);

        serverService.delete(id);
        return ApiResponse.success(null, "서버가 삭제되었습니다");
    }
}

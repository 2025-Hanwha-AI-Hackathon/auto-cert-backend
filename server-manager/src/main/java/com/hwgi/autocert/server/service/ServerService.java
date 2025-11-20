package com.hwgi.autocert.server.service;

import com.hwgi.autocert.common.constants.WebServerType;
import com.hwgi.autocert.common.exception.ResourceNotFoundException;
import com.hwgi.autocert.domain.model.Server;
import com.hwgi.autocert.domain.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 서버 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerService {

    private final ServerRepository serverRepository;

    /**
     * 모든 서버 조회 (페이지네이션)
     */
    public Page<Server> findAll(Pageable pageable) {
        log.debug("Finding all servers - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return serverRepository.findAll(pageable);
    }

    /**
     * ID로 서버 조회
     */
    public Server findById(Long id) {
        log.debug("Finding server by id: {}", id);
        return serverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("서버를 찾을 수 없습니다: " + id));
    }

    /**
     * IP 주소로 서버 조회
     */
    public Server findByIpAddress(String ipAddress) {
        log.debug("Finding server by IP address: {}", ipAddress);
        return serverRepository.findByIpAddress(ipAddress)
                .orElseThrow(() -> new ResourceNotFoundException("서버를 찾을 수 없습니다: " + ipAddress));
    }

    /**
     * 웹서버 타입별 조회
     */
    public Page<Server> findByWebServerType(String webServerType, Pageable pageable) {
        log.debug("Finding servers by web server type: {}", webServerType);
        WebServerType type = WebServerType.fromCode(webServerType);
        return serverRepository.findByWebServerType(type, pageable);
    }

    /**
     * 서버 이름/IP 패턴 검색
     */
    public Page<Server> searchByNameOrIp(String pattern, Pageable pageable) {
        log.debug("Searching servers by name or IP pattern: {}", pattern);
        return serverRepository.searchByNameOrIpPattern(pattern, pageable);
    }

    /**
     * 서버 생성
     */
    @Transactional
    public Server create(String name, String ipAddress, Integer port, WebServerType webServerType,
                        String description, String username, String password, String deployPath) {
        log.info("Creating server: name={}, ip={}, type={}", name, ipAddress, webServerType);

        // IP 주소 중복 확인
        if (serverRepository.existsByIpAddress(ipAddress)) {
            throw new IllegalArgumentException("이미 존재하는 IP 주소입니다: " + ipAddress);
        }

        // password는 필수
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("비밀번호는 필수입니다");
        }

        Server server = Server.builder()
                .name(name)
                .ipAddress(ipAddress)
                .port(port)
                .webServerType(webServerType)
                .description(description)
                .username(username)
                .password(password)
                .deployPath(deployPath)
                .build();

        Server saved = serverRepository.save(server);
        log.info("Server created successfully: id={}, ip={}", saved.getId(), saved.getIpAddress());
        
        return saved;
    }

    /**
     * 서버 수정
     */
    @Transactional
    public Server update(Long id, String name, String ipAddress, Integer port, 
                        WebServerType webServerType, String description, String username,
                        String password, String deployPath) {
        log.info("Updating server: id={}", id);

        Server server = findById(id);

        // IP 주소 변경 시 중복 확인
        if (StringUtils.hasText(ipAddress) && !ipAddress.equals(server.getIpAddress())) {
            if (serverRepository.existsByIpAddress(ipAddress)) {
                throw new IllegalArgumentException("이미 존재하는 IP 주소입니다: " + ipAddress);
            }
            server.setIpAddress(ipAddress);
        }

        // 변경된 필드만 업데이트
        if (StringUtils.hasText(name)) {
            server.setName(name);
        }
        if (port != null) {
            server.setPort(port);
        }
        if (webServerType != null) {
            server.setWebServerType(webServerType);
        }
        if (description != null) {
            server.setDescription(description);
        }
        if (StringUtils.hasText(username)) {
            server.setUsername(username);
        }
        if (password != null) {
            server.setPassword(password);
        }
        if (StringUtils.hasText(deployPath)) {
            server.setDeployPath(deployPath);
        }

        Server updated = serverRepository.save(server);
        log.info("Server updated successfully: id={}", updated.getId());
        
        return updated;
    }

    /**
     * 서버 삭제
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting server: id={}", id);

        Server server = findById(id);
        serverRepository.delete(server);

        log.info("Server deleted successfully: id={}", id);
    }
}


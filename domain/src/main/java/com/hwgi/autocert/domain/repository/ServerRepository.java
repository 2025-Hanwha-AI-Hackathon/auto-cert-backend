package com.hwgi.autocert.domain.repository;

import com.hwgi.autocert.common.constants.WebServerType;
import com.hwgi.autocert.domain.model.Server;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Server Repository
 * 서버 정보 데이터 접근 계층
 */
@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    /**
     * IP 주소로 서버 조회
     */
    Optional<Server> findByIpAddress(String ipAddress);

    /**
     * IP 주소 존재 여부 확인
     */
    boolean existsByIpAddress(String ipAddress);

    /**
     * 서버 이름으로 조회
     */
    Optional<Server> findByName(String name);

    /**
     * 웹서버 타입별 조회
     */
    List<Server> findByWebServerType(WebServerType webServerType);

    /**
     * 웹서버 타입별 페이지 조회
     */
    Page<Server> findByWebServerType(WebServerType webServerType, Pageable pageable);

    /**
     * 서버 이름 패턴 검색
     */
    @Query("SELECT s FROM Server s WHERE s.name LIKE %:pattern% OR s.ipAddress LIKE %:pattern%")
    Page<Server> searchByNameOrIpPattern(@Param("pattern") String pattern, Pageable pageable);
}

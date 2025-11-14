package com.hwgi.autocert.domain.repository;

import com.hwgi.autocert.domain.model.AcmeAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ACME 계정 Repository
 */
@Repository
public interface AcmeAccountRepository extends JpaRepository<AcmeAccount, Long> {

    /**
     * 이메일로 ACME 계정 조회
     */
    Optional<AcmeAccount> findByEmail(String email);

    /**
     * 이메일과 ACME 서버 URL로 계정 조회
     */
    Optional<AcmeAccount> findByEmailAndAcmeServerUrl(String email, String acmeServerUrl);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 상태별 ACME 계정 조회
     */
    List<AcmeAccount> findByStatus(String status);

    /**
     * ACME 서버 URL별 계정 조회
     */
    List<AcmeAccount> findByAcmeServerUrl(String acmeServerUrl);

    /**
     * 활성 계정 중 가장 최근에 사용된 계정 조회 (기본 계정으로 사용)
     */
    @Query("SELECT a FROM AcmeAccount a WHERE a.status = 'ACTIVE' ORDER BY a.lastUsedAt DESC")
    Optional<AcmeAccount> findMostRecentlyUsedActiveAccount();

    /**
     * 특정 ACME 서버의 활성 계정 조회
     */
    @Query("SELECT a FROM AcmeAccount a WHERE a.acmeServerUrl = :serverUrl AND a.status = 'ACTIVE'")
    List<AcmeAccount> findActiveAccountsByServerUrl(@Param("serverUrl") String serverUrl);
}

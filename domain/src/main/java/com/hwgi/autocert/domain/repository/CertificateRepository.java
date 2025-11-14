package com.hwgi.autocert.domain.repository;

import com.hwgi.autocert.domain.model.Certificate;
import com.hwgi.autocert.domain.model.CertificateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 인증서 Repository
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    /**
     * 도메인으로 인증서 조회
     */
    Optional<Certificate> findByDomain(String domain);

    /**
     * 도메인 존재 여부 확인
     */
    boolean existsByDomain(String domain);

    /**
     * 상태별 인증서 조회
     */
    List<Certificate> findByStatus(CertificateStatus status);

    /**
     * 상태별 인증서 페이지 조회
     */
    Page<Certificate> findByStatus(CertificateStatus status, Pageable pageable);

    /**
     * 만료일이 특정 날짜 이전인 인증서 조회 (갱신 대상)
     */
    @Query("SELECT c FROM Certificate c WHERE c.expiresAt <= :expiryDate AND c.status = 'ACTIVE'")
    List<Certificate> findCertificatesExpiringBefore(@Param("expiryDate") LocalDateTime expiryDate);

    /**
     * 만료 임박 인증서 조회 (알림 대상)
     */
    @Query("SELECT c FROM Certificate c WHERE c.expiresAt BETWEEN :now AND :alertDate AND c.status = 'ACTIVE'")
    List<Certificate> findCertificatesNearingExpiry(
            @Param("now") LocalDateTime now,
            @Param("alertDate") LocalDateTime alertDate);

    /**
     * 도메인 패턴으로 검색
     */
    @Query("SELECT c FROM Certificate c WHERE c.domain LIKE %:pattern%")
    Page<Certificate> searchByDomainPattern(@Param("pattern") String pattern, Pageable pageable);
}

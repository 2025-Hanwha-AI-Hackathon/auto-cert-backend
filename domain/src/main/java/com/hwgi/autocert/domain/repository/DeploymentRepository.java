package com.hwgi.autocert.domain.repository;

import com.hwgi.autocert.domain.model.Deployment;
import com.hwgi.autocert.domain.model.DeploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 배포 이력 Repository
 */
@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    /**
     * 인증서별 배포 이력 조회
     */
    Page<Deployment> findByCertificateIdOrderByDeployedAtDesc(Long certificateId, Pageable pageable);

    /**
     * 서버별 배포 이력 조회
     */
    Page<Deployment> findByServerIdOrderByDeployedAtDesc(Long serverId, Pageable pageable);

    /**
     * 상태별 배포 이력 조회
     */
    Page<Deployment> findByStatus(DeploymentStatus status, Pageable pageable);

    /**
     * 특정 기간 내 배포 이력 조회
     */
    @Query("SELECT d FROM Deployment d WHERE d.deployedAt BETWEEN :startDate AND :endDate ORDER BY d.deployedAt DESC")
    List<Deployment> findDeploymentsBetween(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * 최근 배포 이력 조회 (인증서 + 서버)
     */
    @Query("SELECT d FROM Deployment d WHERE d.certificate.id = :certificateId AND d.server.id = :serverId ORDER BY d.deployedAt DESC")
    List<Deployment> findLatestDeployment(@Param("certificateId") Long certificateId, 
                                         @Param("serverId") Long serverId, 
                                         Pageable pageable);
}

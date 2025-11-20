package com.hwgi.autocert.domain.model;

/**
 * 배포 상태
 */
public enum DeploymentStatus {
    /**
     * 배포 성공
     */
    SUCCESS,
    
    /**
     * 배포 실패
     */
    FAILED,
    
    /**
     * 배포 진행 중
     */
    IN_PROGRESS,
    
    /**
     * 배포 롤백됨
     */
    ROLLED_BACK
}

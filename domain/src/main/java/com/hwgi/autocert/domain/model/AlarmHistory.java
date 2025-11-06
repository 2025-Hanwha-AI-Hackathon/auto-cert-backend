package com.hwgi.autocert.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ALARM_HISTORY - 보류
 * 알람 유형: 1. 갱신, 2. 만료임박(스케줄), 3. 만료(스케줄)
 */
@Entity
@Table(name = "alarm_history")
public class AlarmHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false)
    private Certificate certificate;

    @Column(nullable = false)
    private Integer type; // 1: 갱신, 2: 만료임박(스케줄), 3: 만료(스케줄)

    @Column(name = "alarmed_at")
    private LocalDateTime alarmedAt;

    @Column(nullable = false)
    private String status;

    // Getters and Setters
}

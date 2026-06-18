package com.flower.cultivation.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "germination_anomaly")
public class GerminationAnomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sowing_id", nullable = false)
    private Long sowingId;

    @Column(name = "variety_id", nullable = false)
    private Long varietyId;

    @Column(name = "variety_name", nullable = false, length = 100)
    private String varietyName;

    @Column(name = "observation_id")
    private Long observationId;

    @Column(name = "baseline_germination_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal baselineGerminationRate;

    @Column(name = "actual_germination_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal actualGerminationRate;

    @Column(name = "rate_diff", precision = 5, scale = 2, nullable = false)
    private BigDecimal rateDiff;

    @Column(name = "anomaly_level", nullable = false, length = 20)
    private String anomalyLevel;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String actionTaken;

    @Column(length = 50)
    private String handler;

    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    @Column(name = "result_note", columnDefinition = "TEXT")
    private String resultNote;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}

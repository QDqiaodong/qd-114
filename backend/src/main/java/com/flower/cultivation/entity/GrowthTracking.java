package com.flower.cultivation.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "growth_tracking")
public class GrowthTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sowing_id", nullable = false)
    private Long sowingId;

    @Column(name = "stage_code", nullable = false, length = 50)
    private String stageCode;

    @Column(name = "stage_name", nullable = false, length = 50)
    private String stageName;

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    @Column(name = "plant_height", precision = 8, scale = 2)
    private BigDecimal plantHeight;

    @Column(name = "leaf_count")
    private Integer leafCount;

    @Column(name = "root_development", length = 100)
    private String rootDevelopment;

    @Column(name = "health_status", length = 50)
    private String healthStatus;

    @Column(precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(precision = 5, scale = 2)
    private BigDecimal humidity;

    @Column(name = "light_hours", precision = 4, scale = 1)
    private BigDecimal lightHours;

    @Column(length = 200)
    private String fertilization;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}

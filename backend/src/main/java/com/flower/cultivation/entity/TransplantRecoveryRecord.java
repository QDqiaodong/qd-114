package com.flower.cultivation.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transplant_recovery_record")
public class TransplantRecoveryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transplant_id", nullable = false)
    private Long transplantId;

    @Column(name = "sowing_id", nullable = false)
    private Long sowingId;

    @Column(name = "variety_id", nullable = false)
    private Long varietyId;

    @Column(name = "variety_name", nullable = false, length = 100)
    private String varietyName;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "recovery_days", nullable = false)
    private Integer recoveryDays;

    @Column(name = "leaf_status", length = 50)
    private String leafStatus;

    @Column(name = "leaf_count")
    private Integer leafCount;

    @Column(name = "watering_done", nullable = false)
    private Boolean wateringDone;

    @Column(name = "watering_amount", precision = 6, scale = 2)
    private BigDecimal wateringAmount;

    @Column(name = "health_status", length = 50)
    private String healthStatus;

    @Column(precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(precision = 5, scale = 2)
    private BigDecimal humidity;

    @Column(name = "light_hours", precision = 4, scale = 1)
    private BigDecimal lightHours;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}

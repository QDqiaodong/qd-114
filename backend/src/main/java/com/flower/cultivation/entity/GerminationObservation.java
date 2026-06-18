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
@Table(name = "germination_observation")
public class GerminationObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sowing_id", nullable = false)
    private Long sowingId;

    @Column(name = "variety_id", nullable = false)
    private Long varietyId;

    @Column(name = "variety_name", nullable = false, length = 100)
    private String varietyName;

    @Column(name = "observation_date", nullable = false)
    private LocalDate observationDate;

    @Column(name = "germinated_count", nullable = false)
    private Integer germinatedCount;

    @Column(name = "not_germinated_count", nullable = false)
    private Integer notGerminatedCount;

    @Column(name = "germination_rate", precision = 5, scale = 2)
    private BigDecimal germinationRate;

    @Column(name = "anomaly_note", columnDefinition = "TEXT")
    private String anomalyNote;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}

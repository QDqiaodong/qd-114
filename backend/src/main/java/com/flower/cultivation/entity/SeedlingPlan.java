package com.flower.cultivation.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "seedling_plan")
public class SeedlingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seed_id", nullable = false)
    private Long seedId;

    @Column(name = "variety_id", nullable = false)
    private Long varietyId;

    @Column(name = "variety_name", nullable = false, length = 100)
    private String varietyName;

    @Column(name = "germination_days")
    private Integer germinationDays;

    @Column(name = "seedling_days")
    private Integer seedlingDays;

    @Column(name = "shelf_life")
    private Integer shelfLife;

    @Column(name = "planned_sow_date")
    private LocalDate plannedSowDate;

    @Column(name = "planned_transplant_date")
    private LocalDate plannedTransplantDate;

    @Column(name = "expire_date")
    private LocalDate expireDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "actual_sow_date")
    private LocalDate actualSowDate;

    @Column(name = "actual_transplant_date")
    private LocalDate actualTransplantDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}

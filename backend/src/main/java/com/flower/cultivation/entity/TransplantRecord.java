package com.flower.cultivation.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transplant_record")
public class TransplantRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sowing_id", nullable = false)
    private Long sowingId;

    @Column(name = "variety_id", nullable = false)
    private Long varietyId;

    @Column(name = "variety_name", nullable = false, length = 100)
    private String varietyName;

    @Column(name = "transplant_time", nullable = false)
    private LocalDateTime transplantTime;

    @Column(name = "pot_specification", nullable = false, length = 100)
    private String potSpecification;

    @Column(name = "soil_ratio", length = 200)
    private String soilRatio;

    @Column(name = "transplant_quantity", nullable = false)
    private Integer transplantQuantity;

    @Column(name = "recovery_tips", columnDefinition = "TEXT")
    private String recoveryTips;

    @Column(name = "light_requirement", length = 100)
    private String lightRequirement;

    @Column(name = "watering_frequency", length = 100)
    private String wateringFrequency;

    @Column(name = "fertilization_plan", length = 200)
    private String fertilizationPlan;

    @Column(name = "cumulative_quantity")
    private Integer cumulativeQuantity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}

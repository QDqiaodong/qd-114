package com.flower.cultivation.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sowing_record")
public class SowingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seed_id", nullable = false)
    private Long seedId;

    @Column(name = "variety_id", nullable = false)
    private Long varietyId;

    @Column(name = "variety_name", nullable = false, length = 100)
    private String varietyName;

    @Column(name = "sowing_time", nullable = false)
    private LocalDateTime sowingTime;

    @Column(name = "soil_ratio", nullable = false, length = 200)
    private String soilRatio;

    @Column(name = "covering_thickness", precision = 5, scale = 2)
    private BigDecimal coveringThickness;

    @Column(name = "watering_plan", nullable = false, length = 500)
    private String wateringPlan;

    @Column(name = "sowing_quantity", nullable = false)
    private Integer sowingQuantity;

    @Column(name = "container_type", length = 50)
    private String containerType;

    @Column(name = "environment_temp", precision = 5, scale = 2)
    private BigDecimal environmentTemp;

    @Column(name = "environment_humidity", precision = 5, scale = 2)
    private BigDecimal environmentHumidity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}

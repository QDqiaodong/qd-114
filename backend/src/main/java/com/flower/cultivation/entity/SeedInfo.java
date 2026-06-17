package com.flower.cultivation.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "seed_info")
public class SeedInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "variety_id", nullable = false)
    private Long varietyId;

    @Column(name = "variety_name", nullable = false, length = 100)
    private String varietyName;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "acquire_time", nullable = false)
    private LocalDate acquireTime;

    @Column(name = "storage_location", nullable = false, length = 100)
    private String storageLocation;

    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity;

    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    @Column(length = 100)
    private String supplier;

    @Column(name = "shelf_life")
    private Integer shelfLife;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}

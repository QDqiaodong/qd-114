package com.flower.cultivation.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "seed_group")
public class SeedGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "group_code", nullable = false, length = 50, unique = true)
    private String groupCode;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "location", length = 100)
    private String location;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}

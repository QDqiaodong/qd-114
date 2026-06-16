package com.flower.cultivation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransplantRecoveryItemDTO {

    private Long id;
    private Long sowingId;
    private Long varietyId;
    private String varietyName;
    private LocalDateTime transplantTime;
    private Long daysSinceTransplant;
    private String potSpecification;
    private String recoveryTips;
    private String lightRequirement;
    private String wateringFrequency;
    private Integer transplantQuantity;

    private String recoveryStatus;
    private String recoveryStatusText;
    private String recoveryStatusLevel;
    private Boolean needObservation;
    private String observationReason;

    private String latestHealthStatus;
    private LocalDateTime latestTrackingTime;
    private String currentStageName;
    private Boolean hasAbnormal;
    private String latestAbnormalType;
}

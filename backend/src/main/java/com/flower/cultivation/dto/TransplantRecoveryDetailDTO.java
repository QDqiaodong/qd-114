package com.flower.cultivation.dto;

import com.flower.cultivation.entity.TransplantRecoveryRecord;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TransplantRecoveryDetailDTO {

    private Long transplantId;
    private Long sowingId;
    private Long varietyId;
    private String varietyName;
    private LocalDateTime transplantTime;
    private String potSpecification;
    private String recoveryTips;
    private String lightRequirement;
    private String wateringFrequency;
    private Integer transplantQuantity;

    private Long daysSinceTransplant;
    private String recoveryStage;
    private String recoveryStageText;

    private Integer wateringCount;
    private String latestLeafStatus;
    private String latestHealthStatus;
    private Boolean hasAbnormal;
    private LocalDate latestRecordDate;

    private List<TransplantRecoveryRecord> records;
}

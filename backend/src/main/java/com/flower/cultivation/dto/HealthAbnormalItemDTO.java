package com.flower.cultivation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthAbnormalItemDTO {

    private Long trackingId;
    private Long sowingId;
    private Long varietyId;
    private String varietyName;
    private String stageCode;
    private String stageName;
    private String healthStatus;
    private String abnormalType;
    private Integer severityLevel;
    private LocalDateTime recordTime;
    private Integer estimatedSurvival;
}

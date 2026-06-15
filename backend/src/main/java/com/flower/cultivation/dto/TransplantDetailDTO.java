package com.flower.cultivation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransplantDetailDTO {

    private Long id;
    private Long sowingId;
    private Long varietyId;
    private String varietyName;
    private LocalDateTime transplantTime;
    private String potSpecification;
    private String soilRatio;
    private Integer transplantQuantity;
    private Integer cumulativeQuantity;
    private String recoveryTips;
    private String lightRequirement;
    private String wateringFrequency;
    private String fertilizationPlan;
    private String notes;

    private Integer sowingQuantity;
    private Integer estimatedSurvival;
    private Integer previousCumulativeQuantity;

    private PreTransplantGrowth lastGrowthBeforeTransplant;

    @Data
    public static class PreTransplantGrowth {
        private Long id;
        private String stageCode;
        private String stageName;
        private LocalDateTime recordTime;
        private BigDecimal plantHeight;
        private Integer leafCount;
        private String rootDevelopment;
        private String healthStatus;
        private BigDecimal temperature;
        private BigDecimal humidity;
        private BigDecimal lightHours;
        private String fertilization;
        private Integer estimatedSurvival;
        private String notes;
    }
}

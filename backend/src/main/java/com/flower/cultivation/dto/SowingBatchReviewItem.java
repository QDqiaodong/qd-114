package com.flower.cultivation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SowingBatchReviewItem {

    private Long sowingId;
    private LocalDateTime sowingTime;
    private Integer sowingQuantity;
    private String soilRatio;
    private String containerType;

    private Integer actualGerminationDays;
    private Integer estimatedSurvival;
    private BigDecimal seedlingRate;

    private Boolean hasTransplant;
    private Integer transplantQuantity;
    private LocalDateTime transplantTime;

    private List<String> stageRecords;
}

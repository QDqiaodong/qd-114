package com.flower.cultivation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VarietyReviewDTO {

    private Long varietyId;
    private String name;
    private String alias;
    private String category;
    private Integer germinationDays;
    private Integer seedlingDays;
    private String description;

    private Integer totalSowingBatches;
    private Integer totalSowingSeeds;

    private BigDecimal averageGerminationDays;

    private BigDecimal seedlingRate;

    private Integer totalTransplants;
    private Integer totalTransplantQuantity;

    private List<SowingBatchReviewItem> sowingBatches;
}

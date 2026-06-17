package com.flower.cultivation.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeedlingRateStatisticsDTO {

    private Long varietyId;
    private String varietyName;
    private String sourceType;
    private String sowingMonth;

    private Integer totalSowingQuantity;
    private Integer totalTransplantQuantity;
    private BigDecimal seedlingRate;
    private BigDecimal averageGerminationDays;

    private Integer batchCount;
}

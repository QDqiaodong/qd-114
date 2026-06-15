package com.flower.cultivation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeedRiskItemDTO {

    private Long seedId;
    private String varietyName;
    private Long varietyId;
    private Integer remainingQuantity;
    private Integer initialQuantity;
    private LocalDate acquireTime;
    private LocalDate expireDate;
    private Integer shelfLifeMonths;
    private Long daysLeft;
    private String storageLocation;
    private String riskLevel;
    private String riskReason;
}

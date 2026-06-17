package com.flower.cultivation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GroupSeedItemDTO {

    private Long seedId;
    private String varietyName;
    private Long varietyId;
    private String sourceType;
    private String sourceTypeName;
    private LocalDate acquireTime;
    private Integer remainingQuantity;
    private Integer initialQuantity;
    private BigDecimal consumptionRate;
    private Integer sortOrder;
    private String category;
}

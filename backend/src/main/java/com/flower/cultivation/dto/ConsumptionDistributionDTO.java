package com.flower.cultivation.dto;

import lombok.Data;

@Data
public class ConsumptionDistributionDTO {

    private String category;
    private String categoryName;
    private Integer seedCount;
    private Integer totalRemainingQuantity;
    private Integer totalInitialQuantity;
    private Double consumptionRate;
}

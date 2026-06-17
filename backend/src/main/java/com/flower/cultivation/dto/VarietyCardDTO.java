package com.flower.cultivation.dto;

import lombok.Data;

@Data
public class VarietyCardDTO {

    private Long varietyId;
    private String name;
    private String alias;
    private String category;
    private Integer germinationDays;
    private Integer seedlingDays;
    private Integer seedQuantity;
    private Integer activeSowingBatches;
}

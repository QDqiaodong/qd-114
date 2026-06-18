package com.flower.cultivation.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SeedlingPlanGenerateDTO {

    private Long seedId;
    private LocalDate plannedSowDate;
    private String notes;
}

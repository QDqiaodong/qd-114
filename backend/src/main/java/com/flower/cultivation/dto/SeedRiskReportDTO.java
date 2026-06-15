package com.flower.cultivation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeedRiskReportDTO {

    private List<SeedRiskItemDTO> expiredList;
    private List<SeedRiskItemDTO> expiringList;
    private List<SeedRiskItemDTO> overstockedList;
    private Integer expiredCount;
    private Integer expiringCount;
    private Integer overstockedCount;
}

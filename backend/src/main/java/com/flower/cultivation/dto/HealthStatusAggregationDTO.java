package com.flower.cultivation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatusAggregationDTO {

    private Integer totalTrackingCount;
    private Integer abnormalCount;
    private Integer normalCount;
    private Integer unknownCount;
    private Double abnormalRate;

    private List<VarietyHealthDTO> byVariety;
    private List<BatchHealthDTO> byBatch;
    private List<StageHealthDTO> byStage;
    private List<HealthAbnormalItemDTO> abnormalDetails;
}

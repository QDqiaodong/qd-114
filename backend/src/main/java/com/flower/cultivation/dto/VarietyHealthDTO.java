package com.flower.cultivation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VarietyHealthDTO {

    private Long varietyId;
    private String varietyName;
    private Integer trackingCount;
    private Integer abnormalCount;
    private Integer normalCount;
    private Double abnormalRate;

    private List<String> abnormalTypes;
    private List<StageHealthDTO> stageBreakdown;
}

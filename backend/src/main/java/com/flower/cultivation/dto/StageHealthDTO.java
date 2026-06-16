package com.flower.cultivation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageHealthDTO {

    private String stageCode;
    private String stageName;
    private Integer stageOrder;

    private Integer trackingCount;
    private Integer abnormalCount;
    private Integer normalCount;
    private Double abnormalRate;

    private List<String> commonAbnormalTypes;
}

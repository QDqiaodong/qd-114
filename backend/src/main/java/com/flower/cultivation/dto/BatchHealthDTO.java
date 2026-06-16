package com.flower.cultivation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchHealthDTO {

    private Long sowingId;
    private Long varietyId;
    private String varietyName;
    private Integer sowingQuantity;
    private LocalDateTime sowingTime;
    private Long daysSinceSowing;

    private String currentStageCode;
    private String currentStageName;
    private String latestHealthStatus;
    private LocalDateTime latestRecordTime;

    private Integer trackingCount;
    private Integer abnormalCount;
    private Boolean hasAbnormal;

    private List<String> healthStatuses;
}

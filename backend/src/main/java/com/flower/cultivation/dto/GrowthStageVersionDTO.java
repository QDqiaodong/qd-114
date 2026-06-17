package com.flower.cultivation.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GrowthStageVersionDTO {

    private Long version;
    private LocalDateTime updateTime;
    private List<StageItem> stages;

    @Data
    public static class StageItem {
        private String stageCode;
        private String stageName;
        private Integer stageOrder;
        private String description;
    }
}

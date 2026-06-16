package com.flower.cultivation.dto;

import lombok.Data;

import java.util.List;

@Data
public class TransplantRecoveryBoardDTO {

    private Integer totalCount;
    private Integer observingCount;
    private Integer recoveredCount;
    private Integer abnormalCount;

    private List<TransplantRecoveryItemDTO> needObservationItems;
    private List<TransplantRecoveryItemDTO> allItems;

    private List<RecoveryStatByVariety> byVariety;

    @Data
    public static class RecoveryStatByVariety {
        private Long varietyId;
        private String varietyName;
        private Integer totalCount;
        private Integer observingCount;
        private Integer abnormalCount;
    }
}
